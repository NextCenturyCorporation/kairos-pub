#Main TF file; should only have basic configurations for environment
provider "aws" {
  region  = "us-east-1"
  profile = "kairos"
  default_tags {
    tags = {
      Terraform = "experiment-perfeval-nodes-tf"
      Cluster   = local.perfeval.eks_cluster_name
      # Vpc         = local.vpc_name
      # Program     = var.program_name
      # Environment = var.environment
      Experiment = var.experiment
    }
  }
}

# Needs to be configured per profile
# This should not be modified
terraform {
  backend "s3" {
    bucket  = "kairos-tf-state"
    key     = "experiment-perfeval-nodes.tfstate"
    profile = "kairos"
    region  = "us-east-1"
  }
}

data "terraform_remote_state" "perfeval" {
  backend = "s3"
  config = {
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

locals {
  perfeval = data.terraform_remote_state.perfeval.outputs
  env      = data.terraform_remote_state.env.outputs
}
