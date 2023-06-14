provider "aws" {
  region  = "us-east-1"
  profile = "kairos"
  default_tags {
    tags = {
      terraform   = "gitlab-tf"
    }
  }
}

terraform {
  backend "s3" {
    bucket  = "kairos-tf-state"
    key     = "gitlab.tfstate"
    region  = "us-east-1"
    profile = "kairos"
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