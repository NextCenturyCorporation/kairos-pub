resource "aws_acm_certificate" "devbox_cert" {
  domain_name       = aws_route53_record.devbox.fqdn
  validation_method = "DNS"

  tags = {
    Environment = "test"
  }

  lifecycle {
    create_before_destroy = true
  }
}