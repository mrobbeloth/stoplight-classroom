# Terraform State Backend Bootstrap

Tiny standalone stack that creates the S3 bucket and DynamoDB table used as the remote backend for the main `infra/terraform/` stack.

## Why this exists

Terraform's chicken-and-egg problem: a remote backend on S3 needs an S3 bucket, but you can't manage that bucket *with* the same backend you're trying to configure. So this bootstrap stack runs once with **local state**, creates the bucket and lock table, and then the main stack uses them.

## What gets created

- S3 bucket: versioning on, encrypted (AES256), public access blocked, 90-day non-current version expiration
- DynamoDB table: pay-per-request, single-key (`LockID`), used by Terraform for state locking
- Both have `prevent_destroy = true` — losing them means losing the ability to manage your infra

## Cost

- S3: pennies per month (state files are tiny)
- DynamoDB pay-per-request: $0 when idle, fractions of a cent per Terraform operation

Effectively **$0/month**.

## One-time setup

```cmd
cd infra\terraform\bootstrap
copy terraform.tfvars.example terraform.tfvars
:: edit terraform.tfvars and pick a globally-unique state_bucket_name
terraform init
terraform apply
```

Save the output:

```cmd
terraform output backend_config
```

## Wire it into the main stack

Open `infra/terraform/versions.tf`, uncomment the `backend "s3"` block, and replace it with the values from `terraform output backend_config`. Then in the main stack:

```cmd
cd ..
terraform init
```

Terraform will detect the new backend and prompt to migrate state. Say yes (you have nothing in state yet on a fresh setup, so it's a no-op).

## What about the bootstrap's own state?

The `terraform.tfstate` file in this directory is gitignored (covered by the repo-wide `*.tfstate` rule). It holds nothing sensitive — just bucket/table names that are easy to recreate. If you lose it:

- `terraform import` the bucket and table back into a fresh state, or
- accept that the bucket and table exist (they do) and move on; you only need the bootstrap state if you want to *modify* the bucket/table later

For real safety, back up `infra/terraform/bootstrap/terraform.tfstate` to a personal location (not git) after the initial apply.
