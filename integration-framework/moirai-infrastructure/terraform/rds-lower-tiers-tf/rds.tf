resource "aws_db_instance" "development" {
  identifier              = "rds-aws-lower-tier-db"
  engine                  = "mysql"
  instance_class          = "db.t2.medium"
  allocated_storage       = 20
  max_allocated_storage   = 100
  tags                    = { Name = "rds-aws-lower-tier-db" }
  db_name                 = var.rds_database_name_dev
  username                = var.rds_username
  password                = var.db_secret_lower_tier
  parameter_group_name    = "terraform-lower-mysql-8"
  port                    = 3306
  skip_final_snapshot     = true
  publicly_accessible     = true
  db_subnet_group_name    = aws_db_subnet_group.lower-tier.id
  vpc_security_group_ids  = [aws_security_group.default.id]
}

# resource "aws_db_instance" "test" {
#   identifier              = "rds-aws-lower-tier-db"
#   engine                  = "mysql"
#   instance_class          = "db.t2.medium"
#   allocated_storage       = 20
#   max_allocated_storage   = 100
#   tags                    = { Name = "rds-aws-lower-tier-db" }
#   name                    = var.rds_database_name_test
#   username                = var.rds_username
#   password                = var.db_secret
#   parameter_group_name    = "default.mysql5.7"
#   port                    = 3306
#   skip_final_snapshot     = true
#   publicly_accessible     = true
#   db_subnet_group_name    = aws_db_subnet_group.lower-tier.id
#   vpc_security_group_ids  = [aws_security_group.default.id]
# }

# resource "aws_db_instance" "stage" {
#   identifier              = "rds-aws-lower-tier-db"
#   engine                  = "mysql"
#   instance_class          = "db.t2.medium"
#   allocated_storage       = 20
#   max_allocated_storage   = 100
#   tags                    = { Name = "rds-aws-lower-tier-db" }
#   name                    = var.rds_database_name_stage
#   username                = var.rds_username
#   password                = var.db_secret
#   parameter_group_name    = "default.mysql5.7"
#   port                    = 3306
#   skip_final_snapshot     = true
#   publicly_accessible     = true
#   db_subnet_group_name    = aws_db_subnet_group.default.id
#   vpc_security_group_ids  = [aws_security_group.default.id]
# }
