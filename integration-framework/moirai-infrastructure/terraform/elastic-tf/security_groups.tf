resource "aws_security_group" "elisa_access" {
  name        = "elisa_access"
  description = "Inbound rules for external access to elisa machines"
  vpc_id      = local.env.vpc_id

  // Previously we specified the following ingress ports [22, 80, 8008, 8000, 7474] with protocol "tcp"
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    self        = true
    description = "self"
  }

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = var.elisa_cidrs
    description = "corporate ips"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    ignore_changes = [
      # Ignore changes to tags, e.g. because a management agent
      # updates these based on some ruleset managed elsewhere.
      ingress
    ]
  }

  tags = {
    Name = "elisa_access"
    CreatedBy = "terraform"
  }
}
