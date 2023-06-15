resource "aws_db_instance" "default" {
  identifier              = "rds-aws-db"
  engine                  = "mysql"
  instance_class          = "db.t2.medium"
  allocated_storage       = 20
  max_allocated_storage   = 100
  tags                    = { Name = "rds-aws-db" }
  db_name                 = var.rds_database_name
  username                = var.rds_username
  password                = var.db_secret
  parameter_group_name    = "terraform-prod-mysql-8"
  port                    = 3306
  skip_final_snapshot     = true
  publicly_accessible     = true
  db_subnet_group_name    = aws_db_subnet_group.default.id
  vpc_security_group_ids  = [aws_security_group.default.id]
}

