resource "aws_s3_bucket" "kairos-docker-registries" {
  bucket = "kairos-docker-registries"
  acl    = "private"
  force_destroy = true

  tags = {
    Name        = "Performer Registry bucket"
  }
}

