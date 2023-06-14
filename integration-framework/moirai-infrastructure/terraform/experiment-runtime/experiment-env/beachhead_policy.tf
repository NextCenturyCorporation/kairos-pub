resource "aws_iam_role" "k8s_beachhead_access" {
  name               = "k8s_beachhead_access"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
    "Action": "sts:AssumeRole",
    "Principal": {
        "Service": "ec2.amazonaws.com"
    },
    "Effect": "Allow",
    "Sid": ""
    }
  ]
}
EOF

}

resource "aws_iam_instance_profile" "k8s_beachhead_access" {
  name = "k8s_beachhead_access_profile"
  role = "k8s_beachhead_access"
}

resource "aws_iam_role_policy" "k8s_beachhead_access_role_policy" {
  name   = "k8s-beachhead_access_role_policy"
  role   = aws_iam_role.k8s_beachhead_access.id
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
  {
    "Action": [
        "kafka:*",
        "eks:*",
        "ec2:*",
        "cloudwatch:*",
        "cloudformation:*",
        "elasticache:Describe*",
        "elasticloadbalancing:*",
        "autoscaling:*",
        "rds:*",
        "ssm:*",
        "elasticfilesystem:*"
    ],
    "Effect": "Allow",
    "Resource": "*"
  },
  {
      "Effect": "Allow",
      "Action": [
          "ecr:*",
          "cloudtrail:LookupEvents"
      ],
      "Resource": "*"
  },
  {
      "Effect": "Allow",
      "Action": [
          "iam:CreateServiceLinkedRole"
      ],
      "Resource": "*",
      "Condition": {
          "StringEquals": {
              "iam:AWSServiceName": [
                  "replication.ecr.amazonaws.com"
              ]
          }
      }
  },
  {
    "Effect": "Allow",
    "Action": "iam:CreateServiceLinkedRole",
    "Resource": "*",
    "Condition": {
        "StringEquals": {
            "iam:AWSServiceName": [
                "autoscaling.amazonaws.com",
                "ec2scheduled.amazonaws.com",
                "elasticloadbalancing.amazonaws.com",
                "spot.amazonaws.com",
                "spotfleet.amazonaws.com",
                "transitgateway.amazonaws.com"
            ]
        }
    }
  },
  {
    "Effect": "Allow",
    "Action": [
      "elasticloadbalancing:AddListenerCertificates",
      "elasticloadbalancing:AddTags",
      "elasticloadbalancing:CreateListener",
      "elasticloadbalancing:CreateLoadBalancer",
      "elasticloadbalancing:CreateRule",
      "elasticloadbalancing:CreateTargetGroup",
      "elasticloadbalancing:DeleteListener",
      "elasticloadbalancing:DeleteLoadBalancer",
      "elasticloadbalancing:DeleteRule",
      "elasticloadbalancing:DeleteTargetGroup",
      "elasticloadbalancing:DeregisterTargets",
      "elasticloadbalancing:DescribeListenerCertificates",
      "elasticloadbalancing:DescribeListeners",
      "elasticloadbalancing:DescribeLoadBalancers",
      "elasticloadbalancing:DescribeLoadBalancerAttributes",
      "elasticloadbalancing:DescribeRules",
      "elasticloadbalancing:DescribeSSLPolicies",
      "elasticloadbalancing:DescribeTags",
      "elasticloadbalancing:DescribeTargetGroups",
      "elasticloadbalancing:DescribeTargetGroupAttributes",
      "elasticloadbalancing:DescribeTargetHealth",
      "elasticloadbalancing:ModifyListener",
      "elasticloadbalancing:ModifyLoadBalancerAttributes",
      "elasticloadbalancing:ModifyRule",
      "elasticloadbalancing:ModifyTargetGroup",
      "elasticloadbalancing:ModifyTargetGroupAttributes",
      "elasticloadbalancing:RegisterTargets",
      "elasticloadbalancing:RemoveListenerCertificates",
      "elasticloadbalancing:RemoveTags",
      "elasticloadbalancing:SetIpAddressType",
      "elasticloadbalancing:SetSecurityGroups",
      "elasticloadbalancing:SetSubnets",
      "elasticloadbalancing:SetWebACL"
    ],
    "Resource": "*"
  },
    {
      "Effect": "Allow",
      "Action": [
        "iam:CreateServiceLinkedRole",
        "iam:GetServerCertificate",
        "iam:ListServerCertificates"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "cognito-idp:DescribeUserPoolClient"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "waf-regional:GetWebACLForResource",
        "waf-regional:GetWebACL",
        "waf-regional:AssociateWebACL",
        "waf-regional:DisassociateWebACL"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "tag:GetResources",
        "tag:TagResources"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "waf:GetWebACL"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": ["arn:aws:s3:::*",
                    "arn:aws:s3:::*/*"]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": ["arn:aws:s3:::*/*"]
    },
    {
        "Effect": "Allow",
        "Action": ["s3:listAllMyBuckets"],
        "Resource": ["arn:aws:s3:::*"]
    },
    {
        "Effect": "Allow",
        "Action": ["ec2:Describe*"],
        "Resource": "*"
    },
    {
        "Effect": "Allow",
        "Action": [
            "ecr:DescribeImageScanFindings",
            "ecr:GetLifecyclePolicyPreview",
            "ecr:GetDownloadUrlForLayer",
            "ecr:GetAuthorizationToken",
            "ecr:ListTagsForResource",
            "ecr:ListImages",
            "ec2:Describe*",
            "ecr:BatchGetImage",
            "ecr:DescribeImages",
            "ecr:DescribeRepositories",
            "ecr:BatchCheckLayerAvailability",
            "ecr:GetRepositoryPolicy",
            "ecr:GetLifecyclePolicy"
        ],
        "Resource": "*"
    },
    {
            "Effect": "Allow",
            "Action": [
                "iam:*",
                "organizations:DescribeAccount",
                "organizations:DescribeOrganization",
                "organizations:DescribeOrganizationalUnit",
                "organizations:DescribePolicy",
                "organizations:ListChildren",
                "organizations:ListParents",
                "organizations:ListPoliciesForTarget",
                "organizations:ListRoots",
                "organizations:ListPolicies",
                "organizations:ListTargetsForPolicy"
            ],
            "Resource": "*"
        }
  ]
}
EOF

}

# resource "aws_iam_policy_attachment" "k8s_beachhead_route53_policy" {
#   name       = "k8s-beachhead-route53"
#   roles      = [aws_iam_role.k8s_beachhead_access.name]
#   policy_arn = "arn:aws:iam::aws:policy/AmazonRoute53FullAccess"

#   lifecycle {
#     ignore_changes = [roles, users]
#   }
# }

# resource "aws_iam_policy_attachment" "k8s_beachhead_certificate_policy" {
#   name       = "k8s-beachhead-certificate"
#   roles      = [aws_iam_role.k8s_beachhead_access.name]
#   policy_arn = "arn:aws:iam::aws:policy/AWSCertificateManagerFullAccess"

#   lifecycle {
#     ignore_changes = [roles, users]
#   }
# }

# resource "aws_iam_policy_attachment" "k8s_beachhead_ecr_policy" {
#   name       = "k8s-beachhead-ecr"
#   roles      = [aws_iam_role.k8s_beachhead_access.name]
#   policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryFullAccess"
# }
