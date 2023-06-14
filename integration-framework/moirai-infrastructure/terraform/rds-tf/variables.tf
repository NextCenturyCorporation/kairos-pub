variable "rds_username" {
  default     = "pegasus"
  description = "user name for RDS connections"
}
variable "rds_database_name" {
  default     = "riverstyxdb"
  description = "database name hosted on the vpc"
}
variable "db_secret" {
  default     = "SETMEPLEASE"
  description = "Enter password for database user"
}
variable "sg_name" {
  default     = "rds_sg"
  description = "Tag Name for sg"
}
variable "development" {
  description = "dev for Development or prod for Production"
  type        = bool
  default     = "true"
}
locals {
  vpc = data.terraform_remote_state.environment.outputs.vpc
}