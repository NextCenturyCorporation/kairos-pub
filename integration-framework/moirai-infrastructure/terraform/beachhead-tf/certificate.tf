resource "aws_acm_certificate" "beach_head_cert" {
  domain_name       = aws_route53_record.beach_head.fqdn
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}
