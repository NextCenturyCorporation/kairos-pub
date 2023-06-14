resource "aws_iam_user" "user" {
  name = "k8sAccessUser"

  tags = {
    description = "User with access to execute k8s",
    name        = "k8sAccessUser"
  }
}

resource "aws_iam_access_key" "user" {
  user = aws_iam_user.user.name
  #   pgp_key = keybase:aws_iam_user.user.name
}

resource "aws_iam_group" "hippodrome" {
  name = "Hippodrome"
}

resource "aws_iam_group_membership" "hippodrome" {
  name = "hippodrome-membership"

  users = [
    aws_iam_user.user.name
  ]

  group = aws_iam_group.hippodrome.name
}