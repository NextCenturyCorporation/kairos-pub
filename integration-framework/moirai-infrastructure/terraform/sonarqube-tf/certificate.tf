resource "aws_acm_certificate" "sonarqube_cert" {
  domain_name       = aws_route53_record.sonarqube.fqdn
  validation_method = "DNS"

  tags = {
    Environment = "test"
  }

  lifecycle {
    create_before_destroy = true
  }
}