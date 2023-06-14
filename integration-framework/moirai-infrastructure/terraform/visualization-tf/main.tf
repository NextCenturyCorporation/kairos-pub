provider "aws" {
  region  = "us-east-1"
  profile = "kairos"
  default_tags {
    tags = {
      terraform   = "visualization-tf"
    }
  }
}

# Needs to be configured per profile
# This should not be modified
terraform {
  backend "s3" {
    bucket  = "kairos-tf-state"
    key     = "visualization.tfstate"
    profile = "kairos"
    region  = "us-east-1"
  }
}