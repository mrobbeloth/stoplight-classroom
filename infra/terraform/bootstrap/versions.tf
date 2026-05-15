terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
  }

  # NO backend block here — bootstrap intentionally uses local state.
  # The whole point of this stack is to create the resources the *main*
  # stack will use as its remote backend.
}
