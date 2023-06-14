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

variable "beach_head_hostname" {
  type    = string
  default = "zeus"
}

#TODO, do we want conditions?
variable "tier" {
  type        = string
  default     = "production"
  description = "Value options: production, stage, test, development"
}

locals {
  project_root = "${split("moirai-infrastructure", path.cwd)[0]}moirai-infrastructure"
  ansible_dir  = "${local.project_root}/ansible"
  env          = data.terraform_remote_state.environment.outputs
  aws_region   = local.env.aws_region

  username_map            = var.tier != "production" ? "rds_username=\"${data.terraform_remote_state.rds_lower_tier_data.outputs.db_user}\"\n" : "rds_username=\"${data.terraform_remote_state.rds_data.outputs.db_user}\"\n"
  secret_map              = var.tier != "production" ? "rds_secret=\"${data.terraform_remote_state.rds_lower_tier_data.outputs.db_secret}\"\n" : "rds_secret=\"${data.terraform_remote_state.rds_data.outputs.db_secret}\"\n"
  port_map                = "rds_port=\"${data.terraform_remote_state.rds_data.outputs.db_instance_port}\"\n"
  address_map             = var.tier != "production" ? "\"rds_address=${data.terraform_remote_state.rds_lower_tier_data.outputs.db_instance_address}\"\n" : "\"rds_address=${data.terraform_remote_state.rds_data.outputs.db_instance_address}\"\n"
  dbname_map              = var.tier != "production" ? "\"rds_database_name=${data.terraform_remote_state.rds_data.outputs.rds_database_name}${var.tier}\"\n" : "\"rds_database_name=${data.terraform_remote_state.rds_data.outputs.rds_database_name}\"\n"
  aws_user_access_key_map = "\"aws_access_key=${local.env.aws_zeus_access_key}\"\n"
  aws_user_secret_key_map = "\"aws_secret_key=${local.env.aws_zeus_secret}\"\n"
  environment_tier_map    = "\"environment_tier_zeus=${var.tier}\"\n"
  subnet_id_map           = "\"subnet_id=${local.env.vpc.subnets.public[0]}\"\n"
  security_group_id_map   = "\"sg_aws_id=${local.env.sg_developer_access_id}\"\n"
  clotho_ep_map           = "\"CLOTHO_PROXY=https://validation.kairos.nextcentury.com/\"\n"

  production_maps = {
    username = "rds_username=\"${data.terraform_remote_state.rds_data.outputs.db_user}\"\n"
  }

  development_maps = {
    username = "rds_username=\"${data.terraform_remote_state.rds_lower_tier_data.outputs.db_user}\"\n"
  }

  maps = var.tier != "production" ? local.development_maps : local.production_maps

  deploy_zeus_extravars = "${local.clotho_ep_map}${local.username_map}${local.secret_map}${local.port_map}${local.address_map}${local.dbname_map}${local.aws_user_access_key_map}${local.aws_user_secret_key_map}${local.environment_tier_map}${local.subnet_id_map}${local.security_group_id_map}"

  key_pair_name = local.env.machine_key_pair_name
  key_pair_file = "${local.project_root}/key-pairs/${local.key_pair_name}.pem"
}
