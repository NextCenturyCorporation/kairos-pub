resource "aws_acm_certificate" "validation_external_cert" {
  domain_name       = aws_route53_record.validation_external.fqdn
  validation_method = "DNS"

  tags = {
    Environment = "Production"
  }

  lifecycle {
    create_before_destroy = true
  }
}