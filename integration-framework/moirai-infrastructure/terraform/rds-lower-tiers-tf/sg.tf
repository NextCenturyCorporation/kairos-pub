resource "aws_security_group" "default" {
  name        = "main_rds_lower_tiers_subnet_group"
  description = "Our main group of subnets"
  vpc_id      = local.vpc.id

  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "TCP"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = var.sg_name_lower_tier
  }
}

