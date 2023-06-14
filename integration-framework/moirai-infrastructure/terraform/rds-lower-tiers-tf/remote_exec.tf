resource "null_resource" "setup_db" {
  depends_on = [aws_db_instance.development] #wait for the db to be ready
  provisioner "local-exec" {
    command = "mysql -u ${var.rds_username} -p${var.db_secret_lower_tier} -h ${aws_db_instance.development.address} < create-lower-schemas.sql"
  }
}