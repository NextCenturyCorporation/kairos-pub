// Originally gitlab only opened ports 80 and 22, and gitlab runner opened port 22 to approved_cidrs
resource "aws_security_group" "gitlab-services_securitygroup" {
  name        = "Gitlab VPC security group"
  description = "Allow Gitlab instances to interact"
  vpc_id      = local.vpc_id

  ingress {
    from_port       = 0
    to_port         = 0
    protocol        = -1
    self            = true
    security_groups = [local.beach_head_sg]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = ["0.0.0.0/0"]
  }
}
