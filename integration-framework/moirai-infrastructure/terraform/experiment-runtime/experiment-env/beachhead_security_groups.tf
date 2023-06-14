data "aws_instances" "experiment_runtime_access" {
  instance_tags = {
    ExperimentRuntimeAccess = "true"
  }

  instance_state_names = ["running", "stopped"]
}

resource "aws_security_group" "experiment_runtime" {
  name        = "experiment_runtime_sg"
  description = "Inbound rules for developer machines to access instances"
  vpc_id      = local.vpc.id

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
    cidr_blocks = local.approved_cidrs
    description = "corporate and employee ips"
  }

  ingress {
    from_port       = 0
    to_port         = 0
    protocol        = -1
    security_groups = [local.env.sg_developer_access_id]
    description     = "corporate and employee ips"
  }

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = [for k in data.aws_instances.experiment_runtime_access.public_ips : "${k}/32"]
    description = "tagged ExperimentRuntimeAccess"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = -1
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.vpc_name}_developer_access"
  }
}

# resource "aws_security_group_rule" "developer_access_to_default" { #Exposing all from vpc dev access to new sg
#   security_group_id        = module.vpc.default_security_group_id
#   type                     = "ingress"
#   from_port                = 0
#   to_port                  = 0
#   protocol                 = "-1"
#   source_security_group_id = aws_security_group.developer_access.id
#   description              = "developer_access_sg_to_default"
# }

# resource "aws_security_group_rule" "deploy_efs" { ##Exposing 2049 from everywhere in the vpc to new sg
#   type              = "ingress"
#   from_port         = 2049
#   to_port           = 2049
#   protocol          = "tcp"
#   cidr_blocks       = [module.vpc.vpc_cidr_block]
#   security_group_id = module.vpc.default_security_group_id
#   description       = "deploy_efs"
# }
