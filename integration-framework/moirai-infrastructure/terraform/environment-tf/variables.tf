variable "corporate_cidrs" {
  type = list(string)
  default = [
  ]
}

variable "employee_cidrs" {
  type = list(string)
  default = [
    #List of developer IP addresses to whitelist them
  ]
}
  # Term server
    # Public DNS Name: https://ts01-laggar-gcw.cloudsink.net
    # Elastic Load Balancer DNS Name: sensorproxy-laggar-g-524628337.us-gov-west-1.elb.amazonaws.com
    

variable "project_name" {
  type    = string
  default = "kairos"
}

locals {
  approved_cidrs  = concat(var.corporate_cidrs, var.employee_cidrs)
  key_pair        = "moirai-machine"
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
         ,
            {
                "rulePriority": 2,
                "description": "Expire branches older than 60 days",
                "selection": {
                    "tagStatus": "tagged",
                    "tagPrefixList": ["KAIR"],
                    "countType": "sinceImagePushed",
                    "countUnit": "days",
                    "countNumber": 60
                },
                "action": {
                    "type": "expire"
                }
            }
        ]
    }
    EOF
}
