resource "aws_iam_role" "gitlab-backup-role" {
  name               = "gitlab-backup-role"
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

resource "aws_iam_instance_profile" "gitlab-backup" {
  name = "gitlab-backup"
  role = aws_iam_role.gitlab-backup-role.name
}

resource "aws_iam_role_policy" "gitlab-backup-role-policy" {
  name = "gitlab-backup-role-policy"
  role = aws_iam_role.gitlab-backup-role.id

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::${local.gitlab_etc_s3_bucket}",
        "arn:aws:s3:::${local.gitlab_etc_s3_bucket}/*",
        "arn:aws:s3:::${local.gitlab_backup_s3_bucket}",
        "arn:aws:s3:::${local.gitlab_backup_s3_bucket}/*"
      ]
    }
  ]
}
POLICY
}

resource "aws_iam_role" "gitlab_runner_s3" {
  name               = "gitlab-runner-s3"
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

resource "aws_iam_instance_profile" "gitlab_runner_s3" {
  name = "gitlab-runner-s3"
  role = aws_iam_role.gitlab_runner_s3.name
}

resource "aws_iam_role_policy" "gitlab_runner_s3" {
  name = "gitlab-runner-s3"
  role = aws_iam_role.gitlab_runner_s3.id

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::kairos-safe",
        "arn:aws:s3:::kairos-safe/sbin",
        "arn:aws:s3:::kairos-safe/blackduck"
      ]
    }
  ]
}
POLICY
}
