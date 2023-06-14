variable "validation_external_hostname" {
    type = string
    default = "validation"
}

variable "logs_retention_in_days" {
  type        = number
  default     = 90
  description = "Specifies the number of days you want to retain log events"
}

variable "running_env" {
  type        = string
  default     = "production"
}

variable "app" {
    type = string
    default = "Clotho"
}

locals {
  instance_profile = data.terraform_remote_state.environment.outputs.clotho_iam_profile
  zone_id = "Z2EP09JXG7OUE8"
}