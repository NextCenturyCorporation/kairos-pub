variable "hostname" {
    type = string
    default = "devbox"
}

variable "aws_region" {
  default = "us-east-1"
}

variable "aws_availability_zone" {
  default = "us-east-1a"
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

variable "instance_type" {
  description = "AWS instance type."
  default     = "t2.medium"
}

variable "ebs_optimized" {
  default = {
    "t2.micro"     = false,
    "t2.small"     = false,
    "t2.medium"    = false,
    "t2.large"     = false,
    "m4.large"     = true,
    "m4.xlarge"    = true,
    "m4.2 xlarge"  = true,
    "m4.4 xlarge"  = true,
    "m4.10 xlarge" = true,
    "c4.4 xlarge"  = true,
    "c4.8 xlarge"  = true
  }
}

variable "num_instances" {
  default = "1"
}

#TODO, do we want conditions?
variable "tier" {
    type = string
    default = "dev"
    description = "Value options: production, stage, test, development"
}

locals {
  vpc = data.terraform_remote_state.environment.outputs.vpc
  env = data.terraform_remote_state.environment.outputs

  project_root = "${split("moirai-infrastructure", path.cwd)[0]}moirai-infrastructure"

  route_53_zone_id = local.env.route_53_zone_id
  aws_user_access_key_map = "\"aws_access_key=${local.env.aws_zeus_access_key}\"\n"
  aws_user_secret_key_map = "\"aws_secret_key=${local.env.aws_zeus_secret}\"\n"
  subnet_id_map = "\"subnet_id=${local.env.vpc.subnets.public[0]}\"\n"
  security_group_id_map = "\"sg_aws_id=${local.env.sg_developer_access_id}\"\n"
  deply_extravars = "${local.aws_user_access_key_map}${local.aws_user_secret_key_map}${local.subnet_id_map}${local.security_group_id_map}"
  aws_key_pair_name = local.env.machine_key_pair_name
  aws_key_pair_file = "${local.project_root}/key-pairs/${local.aws_key_pair_name}.pem"
}