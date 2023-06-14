#Main TF file; should only have basic configurations for environment
provider "aws" {
  region  = "us-east-1"
  profile = "kairos"
  default_tags {
    tags = {
      Terraform   = "experiment-env-tf"
      Vpc         = local.vpc_name
      Program     = var.program_name
      Environment = var.environment
    }
  }
  # ignore_tags {
  #   key_prefixes = ["kubernetes.io/cluster/"]
  #   keys = ["kubernetes.io/role/internal-elb"]
  # }
}

# Needs to be configured per profile
# This should not be modified
terraform {
  backend "s3" {
    bucket  = "kairos-tf-state"
    key     = "experiment-env.tfstate"
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

