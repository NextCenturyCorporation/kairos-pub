resource "aws_acm_certificate" "elastic_cert" {
  domain_name       = aws_route53_record.elastic.fqdn
  validation_method = "DNS"

  tags = {
    Server = "elastic"
  }

  lifecycle {
    create_before_destroy = true
  }
}