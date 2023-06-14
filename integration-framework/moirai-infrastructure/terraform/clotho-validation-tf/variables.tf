variable "aws_region" {
  default = "us-east-1"
}

variable "aws_availability_zone" {
  default = "us-east-1a"
}

variable "aws_ami" {
  default = "ami-04b9e92b5572fa0d1"
}

variable "instance_type_external" {
  description = "AWS instance type."
  default     = "t3a.large"
}

variable "instance_type_internal" {
  description = "AWS instance type."
  default     = "t3a.medium"
}

variable "instance_type_dev" {
  description = "AWS instance type."
  default     = "m5.2xlarge"
}

variable "validation_external_hostname" {
  type    = string
  default = "validation"
}

variable "validation_dev_hostname" {
  type    = string
  default = "dev.validation"
}

variable "validation_internal_hostname" {
  type    = string
  default = "internal.validation"
}

locals {
  env               = data.terraform_remote_state.environment.outputs
  vpc               = local.env.vpc
  aws_key_pair_name = data.terraform_remote_state.environment.outputs.machine_key_pair_name
  aws_key_pair_file = "../../key-pairs/${local.aws_key_pair_name}.pem"
  instance_profile  = data.terraform_remote_state.environment.outputs.clotho_iam_profile
  zone_id           = "Z2EP09JXG7OUE8"
}
