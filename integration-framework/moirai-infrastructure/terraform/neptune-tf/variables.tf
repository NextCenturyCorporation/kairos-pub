variable "neptune_node_size" {
  type    = string
  default = "db.t4g.medium"
}

variable "neptune_engine_version" {
  type    = string
  default = "1.1.1.0"
}

variable "neptune_port" {
  type    = number
  default = 8182
}

variable "on_create" {
  description = "(when notebook is created)"
  type        = string
  default     = null
}

variable "on_start" {
  description = "(each time notebook is started)"
  type        = string
  default     = null
}

locals {
  env = data.terraform_remote_state.environment.outputs
}
