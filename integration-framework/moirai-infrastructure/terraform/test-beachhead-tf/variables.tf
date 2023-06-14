variable "aws_region" {
  default = "us-east-1"
}

variable "aws_availability_zone" {
  default = "us-east-1a"
}

variable "aws_ami" {
  default = "ami-01de8ddb33de7a3d3"
}

variable "instance_type" {
  description = "AWS instance type."
  default     = "t2.medium"
}

variable "beach_head_hostname" {
  type    = string
  default = "zeus"
}

#TODO, do we want conditions?
variable "tier" {
  type        = string
  default     = "test"
  description = "Value options: production, stage, test, development"
}

locals {
  vpc        = data.terraform_remote_state.environment.outputs.vpc
  env        = data.terraform_remote_state.environment.outputs
  rds_lower  = data.terraform_remote_state.rds_lower_tier_data.outputs
  aws_region = local.env.aws_region

  project_root = "${split("moirai-infrastructure", path.cwd)[0]}moirai-infrastructure"
  ansible_dir  = "${local.project_root}/ansible"

  username_map            = "rds_username=\"${local.rds_lower.db_user}\"\n"
  secret_map              = "rds_secret=\"${local.rds_lower.db_secret}\"\n"
  port_map                = "rds_port=\"${local.rds_lower.db_instance_port}\"\n"
  address_map             = "\"rds_address=${local.rds_lower.db_instance_address}\"\n"
  dbname_map              = "\"rds_database_name=${local.rds_lower.rds_database_test_name}\"\n"
  aws_user_access_key_map = "\"aws_access_key=${local.env.aws_zeus_access_key}\"\n"
  aws_user_secret_key_map = "\"aws_secret_key=${local.env.aws_zeus_secret}\"\n"
  environment_tier_map    = "\"environment_tier_zeus=${var.tier}\"\n"
  subnet_id_map           = "\"subnet_id=${local.env.vpc.subnets.public[0]}\"\n"
  security_group_id_map   = "\"sg_aws_id=${local.env.sg_developer_access_id}\"\n"
  deploy_zeus_extravars   = "${local.username_map}${local.secret_map}${local.port_map}${local.address_map}${local.dbname_map}${local.aws_user_access_key_map}${local.aws_user_secret_key_map}${local.environment_tier_map}${local.subnet_id_map}${local.security_group_id_map}"
  key_pair_name           = data.terraform_remote_state.environment.outputs.machine_key_pair_name
  key_pair_file           = "${local.project_root}/key-pairs/${local.key_pair_name}.pem"
}
