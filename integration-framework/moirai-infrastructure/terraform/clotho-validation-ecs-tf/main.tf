provider "aws" {
  region  = "us-east-1"
  profile = "kairos"
  default_tags {
    tags = {
      Terraform   = "clotho-ecs-validation-tf"
    }
  }
}

# Needs to be configured per profile
# This should not be modified
terraform {
  backend "s3" {
    bucket  = "kairos-tf-state"
    key     = "ecs-clotho-validation.tfstate"
    profile = "kairos"
    region  = "us-east-1"
  }
}

data "terraform_remote_state" "environment" {
  backend = "s3"
  config = {
    bucket  = "kairos-tf-state"
    key     = "environment.tfstate"
    profile = "kairos"
    region  = "us-east-1"
  }
}