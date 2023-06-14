provider "aws" {
  region  = "us-east-1"
  profile = "kairos"
  default_tags {
    tags = {
      terraform   = "docker-repo-tf"
    }
  }
}

# Needs to be configured per profile
# This should not be modified
terraform {
  backend "s3" {
    bucket  = "kairos-tf-state"
    key     = "dockerrepository.tfstate"
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

data "terraform_remote_state" "rds_data" {
  backend = "s3"
  config = {
    bucket  = "kairos-tf-state"
    key     = "rds.tfstate"
    profile = "kairos"
    region  = "us-east-1"
  }
}

data "terraform_remote_state" "rds_lower_tier_data" {
  backend = "s3"
  config = {
    bucket  = "kairos-tf-state"
    key     = "rds-lower-tiers.tfstate"
    profile = "kairos"
    region  = "us-east-1"
  }
}