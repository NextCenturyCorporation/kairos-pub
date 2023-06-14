resource "aws_neptune_cluster" "cluster" {
  cluster_identifier                   = "neptune-cluster"
  engine_version                       = var.neptune_engine_version
  backup_retention_period              = 5
  preferred_backup_window              = "07:00-09:00"
  skip_final_snapshot                  = true
  iam_database_authentication_enabled  = false
  apply_immediately                    = true
  neptune_subnet_group_name            = aws_db_subnet_group.neptune.name
  neptune_cluster_parameter_group_name = aws_neptune_cluster_parameter_group.group.name
  iam_roles                            = [aws_iam_role.neptune_serverless_jupyter.arn]
  vpc_security_group_ids               = [data.terraform_remote_state.environment.outputs.sg_developer_access_id]

  serverless_v2_scaling_configuration { 
    min_capacity = 8
  }
}

resource "aws_neptune_cluster_instance" "writer" {
  cluster_identifier           = aws_neptune_cluster.cluster.id
  availability_zone            = "us-east-1a"
  instance_class               = "db.serverless"
  port                         = var.neptune_port
  neptune_parameter_group_name = "default.neptune1.2"
}

resource "aws_neptune_cluster_instance" "reader" {
  cluster_identifier           = aws_neptune_cluster.cluster.id
  availability_zone            = "us-east-1a"
  instance_class               = "db.serverless"
  port                         = var.neptune_port
  neptune_parameter_group_name = "default.neptune1.2"
}
