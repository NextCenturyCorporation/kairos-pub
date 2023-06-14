resource "aws_ecr_lifecycle_policy" "uipolicy" {
  repository = aws_ecr_repository.protozeus_ecr.name

  policy = local.lifecyclePolicy
}