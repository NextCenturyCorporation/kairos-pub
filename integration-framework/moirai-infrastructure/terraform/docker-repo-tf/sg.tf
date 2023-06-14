resource "aws_security_group" "docker_registry_access" {
  name        = "docker_registry_access"
  description = "Inbound rules for docker registry"
  vpc_id      = local.vpc.id

  //Other can access only port 80
  ingress {
    from_port   = 5000
    to_port     = 5000
    description = ""
    cidr_blocks = ["0.0.0.0/0"]
    protocol    = "tcp"
  }

  ingress {
    from_port   = 5000
    to_port     = 5000
    description = ""
    ipv6_cidr_blocks =["::/0"]
    protocol    = "tcp"
  }
}
