resource "aws_cloudwatch_log_group" "logs" {
  name              = "/fargate/service/clothoCluster-${var.running_env}"
  retention_in_days = var.logs_retention_in_days
  tags              = {}
}