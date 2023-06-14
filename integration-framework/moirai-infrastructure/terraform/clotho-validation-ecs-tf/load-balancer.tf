resource "aws_alb" "main" {
  name = "${var.app}-${var.running_env}"

  # launch lbs in public or private subnets based on "internal" variable
  internal = false
  subnets = [data.terraform_remote_state.environment.outputs.public_1a_id, data.terraform_remote_state.environment.outputs.public_1b_id]
  security_groups = [data.terraform_remote_state.environment.outputs.sg_developer_access_id, data.terraform_remote_state.environment.outputs.sg_external_access_id]

  # enable access logs in order to get support from aws
  access_logs {
    enabled = true
    bucket  = aws_s3_bucket.lb_access_logs.bucket
  }
}

resource "aws_alb_target_group" "main" {
  name                 = "${var.app}-${var.running_env}"
  port                 = "8008"
  protocol             = "HTTP"
  vpc_id               = data.terraform_remote_state.environment.outputs.vpc_id
  target_type          = "ip"
  deregistration_delay = "30"
  health_check {
    path                = ""
    matcher             = 200
    interval            = 30
    timeout             = 10
    healthy_threshold   = 5
    unhealthy_threshold = 5
  }
}

# bucket for storing ALB access logs
resource "aws_s3_bucket" "lb_access_logs" {
  bucket        = "kairos-clotho-lb-access-logs"
  acl           = "private"
  force_destroy = true

  lifecycle_rule {
    id                                     = "cleanup"
    enabled                                = true
    abort_incomplete_multipart_upload_days = 1
    prefix                                 = ""

    expiration {
      days = "30"
    }
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }
}

data "aws_elb_service_account" "main" {
}
