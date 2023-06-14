resource "aws_acm_certificate" "docker_repository_cert" {
  domain_name       = aws_route53_record.docker_repository.fqdn
  validation_method = "DNS"

  tags = {
    Server = "docker_repository"
  }

  lifecycle {
    create_before_destroy = true
  }
}