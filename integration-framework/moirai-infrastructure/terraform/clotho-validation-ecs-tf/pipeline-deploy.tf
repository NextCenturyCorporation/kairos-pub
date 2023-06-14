# data "aws_iam_policy_document" "assume_by_codedeploy" {
#   statement {
#     sid     = ""
#     effect  = "Allow"
#     actions = ["sts:AssumeRole"]

#     principals {
#       type        = "Service"
#       identifiers = ["codedeploy.amazonaws.com"]
#     }
#   }
# }

# resource "aws_iam_role" "codedeploy" {
#   name               = "${var.app}-codedeploy"
#   assume_role_policy = data.aws_iam_policy_document.assume_by_codedeploy.json
# }

# data "aws_iam_policy_document" "codedeploy" {
#   statement {
#     sid    = "AllowLoadBalancingAndECSModifications"
#     effect = "Allow"

#     actions = [
#       "ecs:CreateTaskSet",
#       "ecs:DeleteTaskSet",
#       "ecs:DescribeServices",
#       "ecs:UpdateServicePrimaryTaskSet",
#       "elasticloadbalancing:DescribeListeners",
#       "elasticloadbalancing:DescribeRules",
#       "elasticloadbalancing:DescribeTargetGroups",
#       "elasticloadbalancing:ModifyListener",
#       "elasticloadbalancing:ModifyRule",
#       "lambda:InvokeFunction",
#       "cloudwatch:DescribeAlarms",
#       "sns:Publish",
#       "ecr:*"
#     ]

#     resources = ["*"]
#   }

#   statement {
#     sid    = "AllowPassRole"
#     effect = "Allow"

#     actions = ["iam:PassRole"]

#     resources = [
#       aws_iam_role.ecs_exec_access.arn
#     ]
#   }
# }

# resource "aws_iam_role_policy" "codedeploy" {
#   role   = aws_iam_role.codedeploy.name
#   policy = data.aws_iam_policy_document.codedeploy.json
# }

# resource "aws_codedeploy_app" "this" {
#   compute_platform = "ECS"
#   name             = "${var.app}-service-deploy"
# }

# resource "aws_codedeploy_deployment_group" "this" {
#   app_name               = aws_codedeploy_app.this.name
#   deployment_group_name  = "${var.app}-service-deploy-group"
#   deployment_config_name = "CodeDeployDefault.ECSAllAtOnce"
#   service_role_arn       = aws_iam_role.codedeploy.arn

#   blue_green_deployment_config {
#     deployment_ready_option {
#       action_on_timeout = "CONTINUE_DEPLOYMENT"
#     }

#     terminate_blue_instances_on_deployment_success {
#       action                           = "TERMINATE"
#       termination_wait_time_in_minutes = 60
#     }
#   }

#   ecs_service {
#     cluster_name = aws_ecs_cluster.cluster.name
#     service_name = aws_ecs_service.worker.name
#   }

#   deployment_style {
#     deployment_option = "WITH_TRAFFIC_CONTROL"
#     deployment_type   = "BLUE_GREEN"
#   }

#   load_balancer_info {
#     target_group_pair_info {
#       prod_traffic_route {
#         listener_arns = [aws_alb_listener.https.arn]
#       }

#       target_group {
#         name = aws_alb_target_group.main.*.name[0]
#       }
#     }
#   }
# }