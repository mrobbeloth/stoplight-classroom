terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Optional: configure an S3 backend for shared state. Left commented so first
  # `terraform init` works locally; uncomment after creating the bucket/table.
  #
  # backend "s3" {
  #   bucket         = "stoplight-classroom-tfstate"
  #   key            = "prod/terraform.tfstate"
  #   region         = "us-east-2"
  #   dynamodb_table = "stoplight-classroom-tflock"
  #   encrypt        = true
  # }
}
