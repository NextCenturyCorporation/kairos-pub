resource "aws_ecr_lifecycle_policy" "uipolicy" {
  repository = aws_ecr_repository.ui_ecr.name
  policy = local.lifecyclePolicy
}

resource "aws_ecr_lifecycle_policy" "zeuspolicy" {
  repository = aws_ecr_repository.zeus_ecr.name
  policy = local.lifecyclePolicy
}

resource "aws_ecr_lifecycle_policy" "clothopolicy" {
  repository = aws_ecr_repository.clotho_ecr.name
  policy = local.lifecyclePolicy
}