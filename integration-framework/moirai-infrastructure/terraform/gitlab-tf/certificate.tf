resource "aws_acm_certificate" "gitlab-cert" {
  domain_name       = aws_route53_record.gitlab.fqdn
  validation_method = "DNS"

  tags = {
    Environment = var.env
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate" "gitlab-runner-cert" {
  domain_name       = aws_route53_record.gitlab_runner.fqdn
  validation_method = "DNS"

  tags = {
    Environment = var.env
  }

  lifecycle {
    create_before_destroy = true
  }
}