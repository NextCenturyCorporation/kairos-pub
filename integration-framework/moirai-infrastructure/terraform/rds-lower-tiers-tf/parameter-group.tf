resource "aws_db_parameter_group" "default" {
    name   = "terraform-lower-mysql-8"
    family = "mysql8.0"

    parameter {
        name  = "character_set_server"
        value = "utf8"
    }

    parameter {
        name  = "character_set_client"
        value = "utf8"
    }

    parameter {
        name = "default_password_lifetime"
        value = "0"
    }
}