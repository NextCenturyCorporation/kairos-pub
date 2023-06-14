#Main TF file; should only have basic configurations for environment
provider "aws" {
  region  = "us-east-1"
  profile = "kairos"
  default_tags {
    tags = {
      Terraform   = "experiment-perfeval-tf"
      Vpc         = local.env.vpc.name
      Program     = local.env.program_name
      Environment = local.env.environment
    }
  }
}

# Needs to be configured per profile
# This should not be modified
terraform {
  backend "s3" {
    bucket  = "kairos-tf-state"
    key     = "experiment-perfeval.tfstate"
    profile = "kairos"
    region  = "us-east-1"
  }
}

data "terraform_remote_state" "env" {
  backend = "s3"
  config = {
    bucket  = "kairos-tf-state"
    key     = "experiment-env.tfstate"
    profile = "kairos"
    region  = "us-east-1"
  }
}
