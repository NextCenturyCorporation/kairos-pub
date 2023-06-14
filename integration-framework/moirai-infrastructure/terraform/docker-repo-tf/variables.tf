variable "aws_region" {
  default = "us-east-1"
}

variable "aws_availability_zone" {
  default = "us-east-1a"
}

variable "instance_type" {
  description = "AWS instance type."
  default     = "t3.large"
}

variable "docker_hostname" {
    type = string
    default = "docker"
}

data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-focal-20.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"] # Canonical
}

locals {
    vpc = data.terraform_remote_state.environment.outputs.vpc
    env = data.terraform_remote_state.environment.outputs
    
    project_root = "${split("moirai-infrastructure", path.cwd)[0]}moirai-infrastructure"
    ansible_dir = "${local.project_root}/ansible"

    key_pair_name = data.terraform_remote_state.environment.outputs.machine_key_pair_name
    key_pair_file = "${local.project_root}/key-pairs/${local.key_pair_name}.pem"
}