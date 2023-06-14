variable "beachhead_instance_type" {
  type    = string
  default = "t3a.micro"
}

variable "beachhead_ami_user" {
  type    = string
  default = "ec2-user"
}

locals {
  env                  = data.terraform_remote_state.env.outputs
  project_root         = replace(split("moirai-infrastructure/", path.cwd)[1], "/[^/]+/", "..")
  control_box_hostname = "control.${local.perfevalcluster_name}"
  admin_key_pair_name  = local.env.machine_key_pair_name
  admin_key_pair_file  = "${local.project_root}/key-pairs/${local.admin_key_pair_name}.pem"
}
