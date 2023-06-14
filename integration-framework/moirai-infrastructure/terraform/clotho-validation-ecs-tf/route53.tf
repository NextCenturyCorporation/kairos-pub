resource "aws_route53_record" "validation_external" {
  zone_id = local.zone_id
  name    = var.validation_external_hostname
  type    = "CNAME"
  records = [aws_alb.main.dns_name] # Need to figure out where Public DNS is coming from since there's no ec2 instance to refer to
  ttl     = "300"
}

resource "aws_route53_record" "cert_validation_external" {
  zone_id = local.zone_id
  name    = tolist(aws_acm_certificate.validation_external_cert.domain_validation_options)[0].resource_record_name
  type    = tolist(aws_acm_certificate.validation_external_cert.domain_validation_options)[0].resource_record_type
  records = [tolist(aws_acm_certificate.validation_external_cert.domain_validation_options)[0].resource_record_value]
  ttl     = 60
}