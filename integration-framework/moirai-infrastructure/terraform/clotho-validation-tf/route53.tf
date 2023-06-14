resource "aws_route53_record" "validation_external" {
  zone_id = local.zone_id
  name    = var.validation_external_hostname
  type    = "CNAME"
  records = [aws_instance.validation_external.public_dns]
  ttl     = "300"
}

# resource "aws_route53_record" "validation_dev" {
#   zone_id = local.zone_id
#   name    = var.validation_dev_hostname
#   type    = "CNAME"
#   records = [aws_instance.validation_dev.public_dns]
#   ttl     = "300"
# }

# resource "aws_route53_record" "validation_internal" {
#   zone_id = local.zone_id
#   name    = var.validation_internal_hostname
#   type    = "CNAME"
#   records = [aws_instance.validation_internal.public_dns]
#   ttl     = "300"
# }

resource "aws_route53_record" "cert_validation_external" {
  zone_id = local.zone_id
  name    = tolist(aws_acm_certificate.validation_external_cert.domain_validation_options)[0].resource_record_name
  type    = tolist(aws_acm_certificate.validation_external_cert.domain_validation_options)[0].resource_record_type
  records = [tolist(aws_acm_certificate.validation_external_cert.domain_validation_options)[0].resource_record_value]
  ttl     = 60
}

# resource "aws_route53_record" "cert_validation_dev" {
#   zone_id = local.zone_id
#   name    = tolist(aws_acm_certificate.validation_dev_cert.domain_validation_options)[0].resource_record_name
#   type    = tolist(aws_acm_certificate.validation_dev_cert.domain_validation_options)[0].resource_record_type
#   records = [tolist(aws_acm_certificate.validation_dev_cert.domain_validation_options)[0].resource_record_value]
#   ttl     = 60
# }

# resource "aws_route53_record" "cert_validation_internal" {
#   zone_id = local.zone_id
#   name    = tolist(aws_acm_certificate.validation_internal_cert.domain_validation_options)[0].resource_record_name
#   type    = tolist(aws_acm_certificate.validation_internal_cert.domain_validation_options)[0].resource_record_type
#   records = [tolist(aws_acm_certificate.validation_internal_cert.domain_validation_options)[0].resource_record_value]
#   ttl     = 60
# }
