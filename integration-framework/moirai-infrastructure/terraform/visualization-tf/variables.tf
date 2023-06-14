variable "aws_region" {
  default = "us-east-1"
}

variable "aws_availability_zone" {
  default = "us-east-1a"
}

variable "visualization_hostname" {
  type    = string
  default = "visualization"
}

locals {
  # env               = data.terraform_remote_state.environment.outputs
  # vpc               = local.env.vpc
  # aws_key_pair_name = data.terraform_remote_state.environment.outputs.machine_key_pair_name
  # aws_key_pair_file = "../../key-pairs/${local.aws_key_pair_name}.pem"
  # instance_profile  = data.terraform_remote_state.environment.outputs.clotho_iam_profile
  zone_id           = "Z2EP09JXG7OUE8"

  s3_bucket = "kairos-validation"
  fqdn = "${var.visualization_hostname}.kairos.nextcentury.com"
  s3_origin_id = "myS3Origin"
}
