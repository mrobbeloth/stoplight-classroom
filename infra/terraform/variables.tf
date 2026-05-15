variable "project" {
  description = "Project name, used as a prefix for all resources."
  type        = string
  default     = "stoplight-classroom"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "prod"
}

variable "region" {
  description = "AWS region."
  type        = string
  default     = "us-east-2"
}

# ----- Networking -----

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.40.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (one per AZ)."
  type        = list(string)
  default     = ["10.40.0.0/24", "10.40.1.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (one per AZ)."
  type        = list(string)
  default     = ["10.40.10.0/24", "10.40.11.0/24"]
}

# ----- ECS / Fargate -----

variable "container_image" {
  description = "Full image URI for the Spring Boot app. If empty, defaults to the ECR repo created by this stack with tag :latest."
  type        = string
  default     = ""
}

variable "container_port" {
  description = "Container port the app listens on."
  type        = number
  default     = 8080
}

variable "task_cpu" {
  description = "Fargate task CPU units (256 = 0.25 vCPU, 512 = 0.5 vCPU, 1024 = 1 vCPU)."
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Fargate task memory in MB."
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Desired number of Fargate tasks. Keep at 1 until the WebSocket broker is replaced with an external relay."
  type        = number
  default     = 1
}

# ----- RDS -----

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Initial storage in GB."
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Initial database name."
  type        = string
  default     = "stoplight"
}

variable "db_username" {
  description = "Master DB username."
  type        = string
  default     = "stoplight"
}

variable "db_multi_az" {
  description = "Enable Multi-AZ for the RDS instance."
  type        = bool
  default     = false
}

variable "db_deletion_protection" {
  description = "Enable deletion protection on the RDS instance."
  type        = bool
  default     = true
}

# ----- App secrets / config -----

variable "admin_username" {
  description = "Initial admin username seeded by the application on first start."
  type        = string
  default     = "admin"
}

variable "cors_allowed_origins" {
  description = "Comma-separated list of CORS origin patterns. If empty, defaults to the ALB DNS name (HTTP)."
  type        = string
  default     = ""
}

# ----- TLS / domain (optional) -----

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for the ALB HTTPS listener. If empty, only an HTTP listener is created."
  type        = string
  default     = ""
}

# ----- CI/CD -----

variable "github_repository" {
  description = "GitHub repo in the form 'owner/name' allowed to assume the deploy role via OIDC. Empty disables the role."
  type        = string
  default     = ""
}

# ----- Monitoring / alerts -----

variable "alarm_email" {
  description = "Email address to receive CloudWatch alarm notifications. If empty, the SNS topic is created but no subscription is added."
  type        = string
  default     = ""
}

variable "create_dashboard" {
  description = "Whether to create the CloudWatch dashboard (~$3/mo if you're past the 3-dashboard always-free allowance)."
  type        = bool
  default     = true
}
