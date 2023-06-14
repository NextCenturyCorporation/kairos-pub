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

variable "instance_type" {
  description = "AWS instance type."
  default = "t3.large"
}

variable "hostname" {
  type    = string
  default = "elastic"
}

variable "elisa_cidrs" {
  type = list(string)
  default = [
  ]
}

locals {
    project_root = "${split("moirai-infrastructure", path.cwd)[0]}moirai-infrastructure"
    ansible_dir  = "${local.project_root}/ansible"
    env          = data.terraform_remote_state.environment.outputs
    aws_region   = local.env.aws_region

    key_pair_name = local.env.machine_key_pair_name
    key_pair_file = "${local.project_root}/key-pairs/${local.key_pair_name}.pem"
}