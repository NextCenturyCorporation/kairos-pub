resource "aws_route53_record" "beach_head_test" {
  zone_id = "Z2EP09JXG7OUE8"
  name    = var.tier
  type    = "CNAME"
  records = [aws_instance.beach_head_test.public_dns]
  ttl     = "300"
}
resource "aws_route53_record" "beach_head_test_star" {
  zone_id = "Z2EP09JXG7OUE8"
  name    = "*.${var.tier}"
  type    = "CNAME"
  records = [aws_route53_record.beach_head_test.fqdn]
  ttl     = "300"
}
resource "aws_route53_record" "cert_validation" {
  zone_id = "Z2EP09JXG7OUE8"
  name    = tolist(aws_acm_certificate.beach_head_test_cert.domain_validation_options).0.resource_record_name
  type    = tolist(aws_acm_certificate.beach_head_test_cert.domain_validation_options).0.resource_record_type
  records = [tolist(aws_acm_certificate.beach_head_test_cert.domain_validation_options).0.resource_record_value]
  ttl     = 60
}
