variable "rds_username" {
    default     = "pegasus"
    description = "user name for RDS connections"
}

variable "rds_database_name_dev" {
    default     = "riverstyxdbdevelopment"
    description = "database name hosted on the vpc"
}

variable "rds_database_name_test" {
    default     = "riverstyxdbtest"
    description = "database name hosted on the vpc"
}

variable "rds_database_name_stage" {
    default     = "riverstyxdbstage"
    description = "database name hosted on the vpc"
}

variable "db_secret_lower_tier" {
    default = "SETMEPLEASE"
    description = "Enter password for database user"
}

variable "cidr_blocks" {
    default     = "0.0.0.0/0"
    description = "CIDR for sg"
}

variable "sg_name_lower_tier" {
    default     = "rds_lower_tier_sg"
    description = "Tag Name for sg"
}

variable "development" {
    description =   "dev for Development or prod for Production"
    type        =   bool
    default     =   "true"
}

locals {
  vpc = data.terraform_remote_state.environment.outputs.vpc
}