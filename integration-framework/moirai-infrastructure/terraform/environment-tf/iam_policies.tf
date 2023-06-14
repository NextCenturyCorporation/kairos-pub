resource "aws_iam_policy" "eks_fullaccess" {
  name        = "EKSFullAccess"
  path        = "/"
  description = "Full access to all eks functionality - use carefully"
  policy = jsonencode({
    Version : "2012-10-17",
    Statement : [
      {
        Effect : "Allow",
        Action : "eks:*",
        Resource : "*"
      }
    ]
  })
}

resource "aws_iam_role_policy" "s3_access_role_policy" {
  name   = "s3_access_role_policy"
  role   = aws_iam_role.s3_access.id
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
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
    }
  ]
}
EOF

}

resource "aws_iam_role_policy" "clotho_access_role_policy" {
  name   = "clotho_access_role_policy"
  role   = aws_iam_role.clotho_access.id
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
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
            "route53:*",
            "route53domains:*",
            "elasticloadbalancing:DescribeLoadBalancers"
        ],
        "Resource": "*"
    }
  ]
}
EOF

}

resource "aws_iam_role_policy" "serverless_role_role_policy" {
  name   = "serverless_role_role_policy"
  role   = aws_iam_role.serverless_role.id
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
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
    }
  ]
}
EOF

}

resource "aws_iam_role_policy_attachment" "serverless_role_attach_managedlambda" {
  role       = "serverless_role"
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "serverless_role_attach_manageds3" {
  role       = "serverless_role"
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_user_policy_attachment" "ec2_full_arn" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2FullAccess"
}

# resource "aws_iam_user_policy_attachment" "lambda_full_arn" {
#   user       = aws_iam_user.user.name
#   policy_arn = "arn:aws:iam::aws:policy/AWSLambdaFullAccess"
# }

resource "aws_iam_user_policy_attachment" "eks_full_arn" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_user_policy_attachment" "s3_full_arn" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_user_policy_attachment" "code_deploy_arn" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSCodeDeployRoleForLambda"
}

resource "aws_iam_user_policy_attachment" "eks_server_arn" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSServicePolicy"
}

resource "aws_iam_user_policy_attachment" "rds_full_arn" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonRDSDataFullAccess"
}


resource "aws_iam_user_policy_attachment" "admin_arn" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}
