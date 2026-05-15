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

  # Remote backend. After running the one-time bootstrap stack in
  # `bootstrap/`, copy the values from its `terraform output backend_config`
  # into the block below and uncomment.
  #
  # backend "s3" {
  #   bucket         = "REPLACE-WITH-YOUR-STATE-BUCKET"
  #   key            = "prod/terraform.tfstate"
  #   region         = "us-east-2"
  #   dynamodb_table = "stoplight-classroom-tflock"
  #   encrypt        = true
  # }
}
