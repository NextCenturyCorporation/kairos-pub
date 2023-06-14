variable "env" {
  type    = string
  default = "dev"
}

variable "gitlab_instance_type" {
  type    = string
  default = "t3a.medium"
}

variable "gitlab_volume_size" {
  type    = number
  default = 200
}

variable "runner_volume_size" {
  type    = number
  default = 64
}

variable "runner_instance_type" {
  type    = string
  default = "t3a.medium"
}

variable "instance_ami" {
  type    = string
  default = "ami-04b9e92b5572fa0d1"
  # default = "ami-01de8ddb33de7a3d3"
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
  project_root = "${split("moirai-infrastructure", path.cwd)[0]}moirai-infrastructure"
  ansible_dir  = "${local.project_root}/ansible"
  env          = data.terraform_remote_state.environment.outputs

  project_name     = local.env.project_name
  approved_cidrs   = local.env.approved_cidrs
  aws_region       = local.env.aws_region
  key_pair_name    = local.env.machine_key_pair_name
  key_pair_file    = "${local.project_root}/key-pairs/${local.key_pair_name}.pem"
  route_53_zone_id = local.env.route_53_zone_id
  beach_head_sg    = local.env.sg_developer_access_id
  external_sg      = local.env.sg_external_access_id
  subnet_id        = local.env.vpc.subnets.public[0]
  vpc_id           = local.env.vpc.id

  gitlab_hostname         = "gitlab.${local.env.domain_name}"
  runner_hostname         = "runner.${local.env.domain_name}"
  gitlab_backup_s3_bucket = "${local.project_name}-gitlab-state-backup"
  gitlab_etc_s3_bucket    = "${local.project_name}-gitlab-etc-backup"
}
