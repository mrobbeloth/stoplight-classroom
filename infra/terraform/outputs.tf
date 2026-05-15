output "alb_dns_name" {
  description = "Public DNS name of the ALB. Hit this in a browser once the service is healthy."
  value       = aws_lb.main.dns_name
}

output "ecr_repository_url" {
  description = "ECR repo URL. Push images here as :latest or :<git-sha>."
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name."
  value       = aws_ecs_service.app.name
}

output "ecs_task_definition_family" {
  description = "Task definition family — used by the GitHub Actions deploy job."
  value       = aws_ecs_task_definition.app.family
}

output "rds_endpoint" {
  description = "RDS endpoint (for ops use; reachable only from inside the VPC)."
  value       = aws_db_instance.main.address
}

output "github_deploy_role_arn" {
  description = "ARN of the IAM role GitHub Actions assumes via OIDC. Empty if github_repository was not set."
  value       = try(aws_iam_role.github_deploy[0].arn, "")
}

output "secret_arn" {
  description = "ARN of the consolidated app secret in Secrets Manager (JSON-encoded with db_password, jwt_secret, admin_password)."
  value       = aws_secretsmanager_secret.app.arn
}

output "secret_name" {
  description = "Name of the consolidated app secret. Use with: aws secretsmanager get-secret-value --secret-id <name>"
  value       = aws_secretsmanager_secret.app.name
}

output "alarm_topic_arn" {
  description = "SNS topic ARN that receives alarm notifications. Subscribe additional endpoints to this topic if needed."
  value       = aws_sns_topic.alarms.arn
}

output "dashboard_url" {
  description = "URL to the CloudWatch dashboard (empty if dashboard creation was disabled)."
  value       = var.create_dashboard ? "https://${var.region}.console.aws.amazon.com/cloudwatch/home?region=${var.region}#dashboards:name=${aws_cloudwatch_dashboard.main[0].dashboard_name}" : ""
}
