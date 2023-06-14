output "db_instance_id" {
    value = aws_db_instance.default.id
}

output "db_instance_address" {
    value = aws_db_instance.default.address
}

output "db_instance_port" {
    description = "The port"
    value = aws_db_instance.default.port
}

output "db_user" {
    description = "user"
    value = aws_db_instance.default.username
}

output "db_secret" {
    description = "secret"
    sensitive = true
    value = aws_db_instance.default.password
}

output "rds_database_name" {
    description = "databasen ame"
    value = aws_db_instance.default.db_name
}
