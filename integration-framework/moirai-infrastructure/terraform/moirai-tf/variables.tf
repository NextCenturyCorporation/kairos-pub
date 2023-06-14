variable "aws_region" {
  default = "us-east-1"
}

locals {
  lifecyclePolicy = <<EOF
    {
        "rules": [
            {
                "rulePriority": 1,
                "description": "Expire images older than 1 days",
                "selection": {
                    "tagStatus": "untagged",
                    "countType": "sinceImagePushed",
                    "countUnit": "days",
                    "countNumber": 1
                },
                "action": {
                    "type": "expire"
                }
            }
        ]
    }
    EOF
}
