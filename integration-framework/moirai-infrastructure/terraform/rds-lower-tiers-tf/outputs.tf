output "db_instance_id" {
    value = aws_db_instance.development.id
}

output "db_instance_address" {
    value = aws_db_instance.development.address
}

output "db_instance_port" {
    description = "The port"
    value = aws_db_instance.development.port
}

output "db_user" {
    description = "user"
    value = aws_db_instance.development.username
}

output "db_secret" {
    description = "secret"
    value = aws_db_instance.development.password
}

output "rds_database_name" {
    description = "databasen name"
    value = aws_db_instance.development.name
}

output "rds_database_test_name" {
    description = "databasen name"
    value = var.rds_database_name_test
}