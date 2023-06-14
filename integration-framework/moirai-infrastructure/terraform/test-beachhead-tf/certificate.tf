resource "aws_acm_certificate" "beach_head_test_cert" {
  domain_name       = aws_route53_record.beach_head_test.fqdn
  validation_method = "DNS"

  tags = {
    Environment = "test"
  }

  lifecycle {
    create_before_destroy = true
  }
}
