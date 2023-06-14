
resource "aws_route53_record" "visualization" {
  zone_id = local.zone_id
  name    = var.visualization_hostname
  type    = "CNAME"
  records = [aws_cloudfront_distribution.visualization.domain_name]
  ttl     = "300"
}

resource "aws_route53_record" "cert_visualization" {
  zone_id = local.zone_id
  name    = tolist(aws_acm_certificate.visualization_cert.domain_validation_options)[0].resource_record_name
  type    = tolist(aws_acm_certificate.visualization_cert.domain_validation_options)[0].resource_record_type
  records = [tolist(aws_acm_certificate.visualization_cert.domain_validation_options)[0].resource_record_value]
  ttl     = 60
}
