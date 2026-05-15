# Stoplight Classroom — AWS Deployment

Production deployment to **AWS ECS Fargate + RDS PostgreSQL + ALB**, defined in Terraform.

## What gets created

- VPC (2 public + 2 private subnets across 2 AZs), Internet Gateway, single NAT Gateway
- Application Load Balancer (HTTP listener; HTTPS listener if an ACM cert ARN is supplied)
- ECS Fargate cluster, task definition, service (1 task, 0.5 vCPU / 1 GB)
- ECR repository for the Spring Boot image
- RDS PostgreSQL 16 (`db.t4g.micro`, Single-AZ, encrypted, private)
- Secrets Manager entry: consolidated JSON secret containing DB password, JWT secret, admin password (auto-generated)
- CloudWatch log group `/ecs/stoplight-classroom-prod` (7-day retention)
- IAM roles: ECS task execution, ECS task, GitHub Actions OIDC deploy role
- Security groups limiting traffic: ALB → ECS → RDS
- Monitoring: SNS alarm topic, 5 CloudWatch alarms (ALB health/latency, ECS CPU, RDS CPU/memory), and a CloudWatch dashboard

## Prerequisites

- Terraform ≥ 1.6
- AWS credentials configured locally (`aws configure` or `AWS_PROFILE`)
- The credentials must have **broad permissions** for the initial bootstrap. Terraform creates VPC, IAM roles, RDS, ECS, ECR, ALB, CloudWatch, SNS, and Secrets Manager resources. The simplest path is `AdministratorAccess` on the bootstrap user; you can detach it after the initial deploy since GitHub Actions takes over via the much narrower OIDC role created by this stack. See "If you hit AccessDenied" below.
- Docker Desktop running (only needed for the first manual image push)
- A GitHub repo for this codebase (for CI/CD)

### If you hit AccessDenied

If `terraform apply` fails with something like
`User: ... is not authorized to perform: iam:CreateRole`, your IAM user lacks bootstrap permissions. Quickest fix:

```powershell
aws iam attach-user-policy `
  --user-name <your-iam-user> `
  --policy-arn arn:aws:iam::aws:policy/AdministratorAccess
```

Re-run `terraform apply`. After the stack is up and the GitHub Actions deploy is working, optionally detach the policy:

```powershell
aws iam detach-user-policy `
  --user-name <your-iam-user> `
  --policy-arn arn:aws:iam::aws:policy/AdministratorAccess
```

Day-to-day ops only need permissions for what you're doing (logs, ECS describe/update, Secrets Manager read).

## One-time bootstrap

The cluster needs an image in ECR before tasks can start. Order:

0. **Set up the Terraform state backend** (one-time, ~$0/mo)

   ```cmd
   cd infra\terraform\bootstrap
   copy terraform.tfvars.example terraform.tfvars
   :: edit terraform.tfvars and pick a globally-unique state_bucket_name
   terraform init
   terraform apply
   terraform output backend_config
   ```

   Paste the printed `backend "s3" { ... }` block into `infra/terraform/versions.tf` (replace the commented stub) and uncomment it. See `bootstrap/README.md` for full details.

1. **Apply infra (creates ECR, but service will fail to start since no image exists yet)**

   ```cmd
   cd infra\terraform
   copy terraform.tfvars.example terraform.tfvars
   :: edit terraform.tfvars and set github_repository
   terraform init
   terraform apply -target="aws_ecr_repository.app"
   ```

   > **Note on quoting:** the double quotes around the target are required in PowerShell (which interprets the `.` as a property access) and harmless in `cmd`. Without them you get `Error: Invalid target "aws_ecr_repository"`.

   > **Alternative — skip the targeted apply.** If you'd rather avoid the targeted-apply warning Terraform prints, run a full `terraform apply` instead. The ECS service will fail to start (no image) but ECR, RDS, ALB, and everything else come up. Push the first image (step 2), then run `aws ecs update-service --cluster stoplight-classroom-prod-cluster --service stoplight-classroom-prod-svc --force-new-deployment --region us-east-2` to retry. Slightly more total wall-clock time (~10 extra minutes for RDS to come up before the failure surfaces), but cleaner.

2. **Build and push the first image manually**

   The commands below need your AWS account ID. Pick the block matching your shell.

   **PowerShell:**

   ```powershell
   $ACCOUNT_ID = aws sts get-caller-identity --query Account --output text
   $REGION = "us-east-2"
   $REPO = "stoplight-classroom-prod"
   $REGISTRY = "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"

   aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $REGISTRY
   docker build -t "${REPO}:latest" .
   docker tag "${REPO}:latest" "$REGISTRY/${REPO}:latest"
   docker push "$REGISTRY/${REPO}:latest"
   ```

   **cmd:**

   ```cmd
   for /f "tokens=*" %i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%i
   set REGION=us-east-2
   set REPO=stoplight-classroom-prod
   set REGISTRY=%ACCOUNT_ID%.dkr.ecr.%REGION%.amazonaws.com

   aws ecr get-login-password --region %REGION% | docker login --username AWS --password-stdin %REGISTRY%
   docker build -t %REPO%:latest .
   docker tag %REPO%:latest %REGISTRY%/%REPO%:latest
   docker push %REGISTRY%/%REPO%:latest
   ```

   Run from the **repo root**, not from `infra\terraform\`.

3. **Apply the rest of the stack**

   ```cmd
   cd infra\terraform
   terraform apply
   ```

   This brings up the VPC, RDS, ALB, ECS service, etc. RDS takes ~10 minutes; the ECS service waits for it.

4. **Verify**

   ```cmd
   terraform output alb_dns_name
   ```

   Open `http://<alb-dns-name>` in a browser. Hit `http://<alb-dns-name>/actuator/health` to confirm the app is up.

5. **Get the seeded admin password**

   ```cmd
   aws secretsmanager get-secret-value ^
     --secret-id stoplight-classroom-prod/app ^
     --query SecretString --output text
   ```

   That returns JSON. Pull `admin_password` out of it (jq, PowerShell `ConvertFrom-Json`, or just eyeball it). Log in at `/login` with username `admin` and that password. Change it.

## Configure GitHub Actions for continuous deploys

After `terraform apply` finishes:

1. Grab the role ARN:

   ```cmd
   terraform output github_deploy_role_arn
   ```

2. In GitHub: **Settings → Secrets and variables → Actions → New repository secret**

   - Name: `AWS_DEPLOY_ROLE_ARN`
   - Value: the ARN from above

3. Push to `main`. The `deploy` job will:
   - Build and push the image to ECR (tagged with both `latest` and the git SHA)
   - Render a new task definition with the new image
   - Update the ECS service and wait for stability

## Routine ops

| Task | Command |
|---|---|
| Tail app logs | `aws logs tail /ecs/stoplight-classroom-prod --follow --region us-east-2` |
| Force a new deploy without code change | `aws ecs update-service --cluster stoplight-classroom-prod-cluster --service stoplight-classroom-prod-svc --force-new-deployment --region us-east-2` |
| Scale up | edit `desired_count` in `terraform.tfvars`, but **read the WebSocket caveat first** |
| Rotate JWT secret | update the secret in Secrets Manager, then force a new deploy |
| Connect to RDS | use a bastion or Session Manager tunnel; RDS is private |
| Open the dashboard | `terraform output dashboard_url` |
| Subscribe another email to alarms | `aws sns subscribe --topic-arn $(terraform output -raw alarm_topic_arn) --protocol email --notification-endpoint name@example.com` |

## Monitoring & alerts

Five CloudWatch alarms are wired to an SNS topic, with one email subscription set via `alarm_email` in `terraform.tfvars`:

| Alarm | Triggers when | Meaning |
|---|---|---|
| ALB no healthy targets | `HealthyHostCount < 1` for 2 min | App is down |
| ALB p95 latency high | `TargetResponseTime` p95 > 1s for 10 min | Slowness — investigate DB / task CPU |
| ECS CPU high | `CPUUtilization` > 70% for 15 min | Time to bump `task_cpu` |
| RDS CPU high | `CPUUtilization` > 70% for 15 min | Time to upsize the DB instance |
| RDS memory low | `FreeableMemory` < 100 MB for 10 min | DB is under memory pressure |

After `terraform apply`, AWS sends a confirmation email — click the link or no alarms reach you. Add more recipients with the `aws sns subscribe` command in the ops table above.

The dashboard groups ALB requests/5XX, latency percentiles, target health, ECS CPU/memory, and RDS CPU/memory/connections into one view. Run `terraform output dashboard_url` to get the link.

To skip the dashboard (save ~$3/mo if you're past the always-free 3-dashboard allowance), set `create_dashboard = false` in `terraform.tfvars`.

## WebSocket scaling caveat

The Spring app uses Spring's in-memory STOMP broker (`enableSimpleBroker`). Multiple Fargate tasks will not share WebSocket state — a teacher and their students could land on different tasks and miss each other's messages.

ALB sticky sessions are enabled in this stack, which keeps a given client pinned to one task and is fine for the **single-task** configuration used here. To run more than one task, swap `enableSimpleBroker` for an external broker (Amazon MQ for RabbitMQ or a self-hosted ActiveMQ) and use `enableStompBrokerRelay`. That work is tracked separately.

## TLS / custom domain

To enable HTTPS with a Route 53 domain:

1. Set `domain_name` in `terraform.tfvars`:

   ```hcl
   domain_name = "stoplightonline.com"
   ```

2. Run `terraform apply`. This will:
   - Look up the Route 53 hosted zone
   - Request an ACM certificate for the domain (+ www subdomain)
   - Create DNS validation records (automatic, no console clicks)
   - Wait for the cert to validate (~2–5 min)
   - Add A/AAAA alias records pointing at the ALB
   - Create an HTTPS listener on the ALB
   - Redirect all HTTP → HTTPS

3. After apply finishes, `terraform output app_url` gives you `https://stoplightonline.com`.

4. The CORS origin is auto-detected from the domain — no need to set `cors_allowed_origins` unless you have additional origins.

**Note:** DNS propagation takes a few minutes. The cert validation is usually fast (<5 min) since both the cert and the validation records are in the same AWS account.

To use a manually-provisioned ACM cert (e.g., cross-account or from a different CA), skip `domain_name` and set `acm_certificate_arn` instead. You'll need to create DNS records manually in that case.

## Cost estimate (us-east-2, on-demand, ballpark)

| Component | Monthly |
|---|---|
| Fargate (0.5 vCPU + 1 GB, 24/7) | ~$15 |
| RDS db.t4g.micro Single-AZ + 20 GB gp3 | ~$15 |
| ALB | ~$18 + LCU |
| NAT Gateway | ~$32 + data |
| Secrets Manager (1 consolidated secret) | ~$0.40 |
| CloudWatch Logs (light, 7-day retention) | ~$0.50 |
| CloudWatch alarms (5) | ~$0 (always-free covers up to 10) |
| CloudWatch dashboard (1) | ~$0 (always-free covers up to 3) |
| SNS (1 topic + 1 email subscription) | ~$0 |
| Terraform state (S3 versioned + DynamoDB on-demand) | ~$0 |
| **Total baseline** | **~$80 / month** |

NAT Gateway is the biggest avoidable cost. To remove it, add VPC endpoints for ECR (api + dkr), CloudWatch Logs, Secrets Manager, and S3 — that's a follow-up.

## Tearing it all down

```cmd
cd infra\terraform
terraform destroy
```

If `db_deletion_protection = true` (default), set it to `false` and apply once before destroying, or AWS will refuse to delete the RDS instance.
