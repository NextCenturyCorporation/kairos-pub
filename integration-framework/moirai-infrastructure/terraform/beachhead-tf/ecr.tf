resource "aws_ecr_repository" "genesis_ecr" {
  name                 = "genesis"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = false
  }
}