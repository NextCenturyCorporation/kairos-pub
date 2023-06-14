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
  iam_roles                            = [aws_iam_role.neptune_jupyter.arn]
}

resource "aws_neptune_cluster_instance" "node" {
  count              = 3 // defaults 1 writer and rest readers
  cluster_identifier = aws_neptune_cluster.cluster.id
  engine_version     = var.neptune_engine_version
  instance_class     = var.neptune_node_size
  port               = var.neptune_port

  apply_immediately = true
}
