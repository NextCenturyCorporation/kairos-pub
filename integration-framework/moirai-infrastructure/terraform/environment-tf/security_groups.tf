resource "aws_security_group" "developer_access" {
  name        = "developer_access"
  description = "Inbound rules for developer machines to access instances"
  vpc_id      = module.vpc.vpc_id

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
    cidr_blocks = var.corporate_cidrs
    description = "corporate ips"
  }

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = var.employee_cidrs
    description = "employee ips"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "developer_access"
    CreatedBy = "terraform"
  }
}

resource "aws_security_group" "external_access" {
  name        = "external_access"
  description = "Inbound rules for external machines to access instances"
  vpc_id      = module.vpc.vpc_id

  //Other can access only port 80
  ingress {
    from_port   = 80
    to_port     = 80
    description = ""
    cidr_blocks = ["0.0.0.0/0"]
    protocol    = "tcp"
  }

  ingress {
    from_port   = 80
    to_port     = 80
    description = ""
    ipv6_cidr_blocks =["::/0"]
    protocol    = "tcp"
  }

  ingress {
    from_port   = 443
    to_port     = 443
    description = ""
    cidr_blocks = ["0.0.0.0/0"]
    protocol    = "tcp"
  }

  ingress {
    from_port   = 443
    to_port     = 443
    description = ""
    ipv6_cidr_blocks =["::/0"]
    protocol    = "tcp"
  }

  tags = {
    Name = "external_access"
    CreatedBy = "terraform"
  }
}
