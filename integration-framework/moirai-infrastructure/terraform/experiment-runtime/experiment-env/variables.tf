variable "aws_region" {
  default = "us-east-1"
}

variable "program_name" {
  type    = string
  default = "kairos"
}

variable "environment" {
  type    = string
  default = "q9"
}

locals {
  project_root        = "${split("moirai-infrastructure", path.cwd)[0]}moirai-infrastructure"
  approved_cidrs      = data.terraform_remote_state.environment.outputs.approved_cidrs
  admin_key_pair_name = data.terraform_remote_state.environment.outputs.machine_key_pair_name
  admin_key_pair_file = "${local.project_root}/key-pairs/${local.admin_key_pair_name}.pem"

  env = data.terraform_remote_state.environment.outputs
  vpc = data.terraform_remote_state.environment.outputs.vpc
}

locals {
  perfevalcluster_name = "hippodrome"
  vpc_name             = "${local.perfevalcluster_name}-VPC"
}

locals {
  kafka_config = {
    cluster_name         = "${local.perfevalcluster_name}-kafka"
    version              = "2.8.0"
    instance_type        = "kafka.t3.small"
    node_count           = 3
    volume_size          = 100
    brokerazdistribution = "DEFAULT"
    encryption_spec      = "PLAINTEXT"
    broker_monitoring    = "PER_TOPIC_PER_BROKER"
  }
}
