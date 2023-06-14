resource "aws_iam_role" "sonarqube_access" {
  name               = "sonarqube_access"
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

resource "aws_iam_instance_profile" "sonarqube_access" {
  name = "sonarqube_accesss_profile"
  role = "sonarqube_access"
}

resource "aws_iam_role_policy" "sonarqube_access_role_policy" {
  name   = "sonarqube_access_role_policy"
  role   = aws_iam_role.sonarqube_access.id
  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [{
        "Action": "ec2:*",
        "Effect": "Allow",
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
    },
    {
        "Effect": "Allow",
        "Action": [
            "route53:*",
            "route53domains:*"
        ],
        "Resource": "*"
    },
    {
        "Effect": "Allow",
        "Action": "apigateway:GET",
        "Resource": "arn:aws:apigateway:*::/domainnames"
    }
  ]
}
EOF

}
