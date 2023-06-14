resource "aws_iam_role" "neptune_jupyter" {
  name               = "neptune_jupyter"
  assume_role_policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
        "Effect": "Allow",
        "Principal": {
            "Service": [
                "sagemaker.amazonaws.com",
                "rds.amazonaws.com",
                "s3.us-east-1.amazonaws.com"
            ]
        },
        "Action": "sts:AssumeRole"
        }
    ]
}
EOF

}

resource "aws_iam_role_policy_attachment" "s3_access_attach" {
  role       = aws_iam_role.neptune_jupyter.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_role_policy_attachment" "rds_access_attach" {
  role       = aws_iam_role.neptune_jupyter.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonRDSFullAccess"
}

resource "aws_iam_role_policy_attachment" "sagemaker_access_attach" {
  role       = aws_iam_role.neptune_jupyter.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSageMakerFullAccess"
}

resource "aws_iam_role_policy_attachment" "iam_access_attach" {
  role       = aws_iam_role.neptune_jupyter.name
  policy_arn = "arn:aws:iam::aws:policy/IAMFullAccess"
}

resource "aws_iam_instance_profile" "neptune_jupyter" {
  name = "neptune_jupyter_profile"
  role = "neptune_jupyter"
}
