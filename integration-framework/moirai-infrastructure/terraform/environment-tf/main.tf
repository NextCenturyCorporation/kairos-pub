#Main TF file; should only have basic configurations for environment
provider "aws" {
  region  = "us-east-1"
  profile = "kairos"
  default_tags {
    tags = {
      Terraform   = "environment-tf"
      Program     = var.project_name
      Environment = "all"
    }
  }
  ignore_tags {
    key_prefixes = ["kubernetes.io/cluster/"]
    keys         = ["kubernetes.io/role/internal-elb","kubernetes.io/role/elb"]
  }
}

# Needs to be configured per profile
# This should not be modified
terraform {
  backend "s3" {
    bucket  = "kairos-tf-state"
    key     = "environment.tfstate"
    profile = "kairos"
    region  = "us-east-1"
  }
}
