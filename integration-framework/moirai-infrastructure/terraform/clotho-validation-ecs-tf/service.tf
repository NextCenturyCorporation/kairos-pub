resource "aws_ecs_service" "worker" {
  name            = "clotho-service"
  cluster         = aws_ecs_cluster.cluster.id
  task_definition = aws_ecs_task_definition.task_definition.arn
  desired_count   = 1

  launch_type = "FARGATE"

  network_configuration {
      subnets = [data.terraform_remote_state.environment.outputs.private_1a_id, data.terraform_remote_state.environment.outputs.public_1a_id, data.terraform_remote_state.environment.outputs.public_1b_id]
      security_groups = [data.terraform_remote_state.environment.outputs.sg_developer_access_id, data.terraform_remote_state.environment.outputs.sg_external_access_id]
      assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_alb_target_group.main.arn
    container_name   = "${var.app}-${var.running_env}"
    container_port   = "8008"
  }

  depends_on      = [aws_alb_listener.https]

  lifecycle {
    ignore_changes = [task_definition]
  }
}
