variable "hostname" {
    type = string
    default = "sonarqube"
}

variable "aws_key_name" {
	default = "moirai-machine"
}
variable "aws_key_file" {
	default = "../key-pairs/moirai-machine.pem"
}

variable "aws_region" {
  	default = "us-east-1"
}

variable "aws_availability_zone" {
  	default = "us-east-1a"
}

variable "instance_ami" {
    type = string
    default = "ami-03e69a5ed5a7c9fe1"
}

variable "instance_type" {
  description = "AWS instance type."
  default = "t3a.medium"
}

variable "sonarqube_external_hostname" {
  type    = string
  default = "sonarqube"
}

variable "sonarqube_dev_hostname" {
  type    = string
  default = "dev.sonarqube"
}

variable "sonarqube_internal_hostname" {
  type    = string
  default = "internal.sonarqube"
}

# locals {
#     project_name = data.terraform_remote_state.environment.outputs.project_name
#     approved_cidrs = data.terraform_remote_state.environment.outputs.approved_cidrs
#     aws_region = data.terraform_remote_state.environment.outputs.aws_region
#     key_pair_name = data.terraform_remote_state.environment.outputs.admin_key_pair_name
#     route_53_zone_id = data.terraform_remote_state.environment.outputs.route_53_zone_id
#     beach_head_sg = data.terraform_remote_state.environment.outputs.sg_developer_access_id
#     subnet_id = data.terraform_remote_state.environment.outputs.public_1a_id
#     vpc_id = data.terraform_remote_state.environment.outputs.vpc_id
#     zone_id = "Z2EP09JXG7OUE8"
# }
locals {
    project_root = "${split("moirai-infrastructure", path.cwd)[0]}moirai-infrastructure"
    project_name = data.terraform_remote_state.environment.outputs.project_name

    ansible_dir = "${local.project_root}/ansible"
    env          = data.terraform_remote_state.environment.outputs
    aws_region   = local.env.aws_region

    approved_cidrs = data.terraform_remote_state.environment.outputs.approved_cidrs
    beach_head_sg = data.terraform_remote_state.environment.outputs.sg_developer_access_id
    external_sg = data.terraform_remote_state.environment.outputs.sg_external_access_id

    key_pair_name = data.terraform_remote_state.environment.outputs.machine_key_pair_name
    key_pair_file = "${local.project_root}/key-pairs/${local.key_pair_name}.pem"
    route_53_zone_id = data.terraform_remote_state.environment.outputs.route_53_zone_id
    sonarqube_hostname = "sonarqube.${data.terraform_remote_state.environment.outputs.domain_name}"
    subnet_id = data.terraform_remote_state.environment.outputs.vpc.subnets.public[0]
    vpc = data.terraform_remote_state.environment.outputs.vpc
    zone_id = "Z2EP09JXG7OUE8"
}
