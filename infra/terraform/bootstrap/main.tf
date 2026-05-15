provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = var.project
      ManagedBy = "terraform"
      Purpose   = "tfstate-backend"
    }
  }
}

variable "project" {
  description = "Project name, used as a prefix for resources."
  type        = string
  default     = "stoplight-classroom"
}

variable "region" {
  description = "AWS region for the state bucket and lock table."
  type        = string
  default     = "us-east-2"
}

variable "state_bucket_name" {
  description = "Globally-unique S3 bucket name for Terraform state. S3 bucket names must be globally unique across all AWS accounts."
  type        = string
}

# ----- S3 bucket for state -----

resource "aws_s3_bucket" "tfstate" {
  bucket = var.state_bucket_name

  # Defensive: prevent accidental destroy of state. If you really need to
  # delete this bucket, set lifecycle.prevent_destroy = false and re-apply first.
  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Auto-expire old state versions to control storage cost.
resource "aws_s3_bucket_lifecycle_configuration" "tfstate" {
  bucket = aws_s3_bucket.tfstate.id

  rule {
    id     = "expire-noncurrent-versions"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = 90
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# ----- DynamoDB table for state locking -----

resource "aws_dynamodb_table" "tflock" {
  name         = "${var.project}-tflock"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  point_in_time_recovery {
    enabled = false
  }

  lifecycle {
    prevent_destroy = true
  }
}

output "state_bucket" {
  description = "S3 bucket holding Terraform state. Use this in the main stack's backend config."
  value       = aws_s3_bucket.tfstate.bucket
}

output "lock_table" {
  description = "DynamoDB table for state locking. Use this in the main stack's backend config."
  value       = aws_dynamodb_table.tflock.name
}

output "backend_config" {
  description = "Copy/paste this into the main stack's backend block."
  value = <<-EOT
    backend "s3" {
      bucket         = "${aws_s3_bucket.tfstate.bucket}"
      key            = "prod/terraform.tfstate"
      region         = "${var.region}"
      dynamodb_table = "${aws_dynamodb_table.tflock.name}"
      encrypt        = true
    }
  EOT
}
