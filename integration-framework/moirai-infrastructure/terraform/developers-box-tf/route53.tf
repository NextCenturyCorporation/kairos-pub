resource "aws_route53_record" "devbox" {
  zone_id = local.route_53_zone_id
  name    = var.hostname
  type    = "CNAME"
  records = [aws_instance.developers_box[0].public_dns]
  ttl     = "300"
}

resource "aws_route53_record" "cert_validation" {
  zone_id = local.route_53_zone_id
  name    = tolist(aws_acm_certificate.devbox_cert.domain_validation_options)[0].resource_record_name
  type    = tolist(aws_acm_certificate.devbox_cert.domain_validation_options)[0].resource_record_type
  records = [tolist(aws_acm_certificate.devbox_cert.domain_validation_options)[0].resource_record_value]
  ttl     = 60
}