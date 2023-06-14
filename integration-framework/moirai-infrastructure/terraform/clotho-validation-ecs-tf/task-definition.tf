resource "aws_ecs_task_definition" "task_definition" {
  family = "ecs-clotho-taskdef"
                            # task defination json file location
  execution_role_arn       = aws_iam_role.ecs_exec_access.arn 
  task_role_arn            = aws_iam_role.ecs_exec_access.arn                                                                      # TASK running role
  requires_compatibilities = ["FARGATE"]
  network_mode = "awsvpc"
  cpu = 4096
  memory = 30720

  depends_on = [
    aws_iam_role.ecs_exec_access
  ]

  container_definitions    = jsonencode([
    {
      name      = "${var.app}-${var.running_env}"
      # image     = "130602597458.dkr.ecr.us-east-1.amazonaws.com/genesis:clotho-latest"
      image     = "130602597458.dkr.ecr.us-east-1.amazonaws.com/clotho:development" // Image to deploy hosted on AWS ECR
      cpu       = 2048
      memory    = 15360
      essential = true
      portMappings = [
        {
          containerPort = 8008
          hostPort      = 8008
        }
      ]
      environment = [
          {
              name = "DB_TYPE"
              value = "validation"
          }
      ]
      logConfiguration: {
        logDriver: "awslogs",
        options: {
            awslogs-group: "/fargate/service/clothoCluster-${var.running_env}",
            awslogs-region: "us-east-1",
            awslogs-stream-prefix: "ecs"
        }
      }   
    }
  ])   
} 