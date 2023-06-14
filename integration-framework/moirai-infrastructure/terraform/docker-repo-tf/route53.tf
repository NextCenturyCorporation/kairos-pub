resource "aws_route53_record" "docker_repository" {
  zone_id = "Z2EP09JXG7OUE8"
  name    = var.docker_hostname
  type    = "CNAME"
  records = [aws_instance.docker_repository.public_dns]
  ttl     = "300"
}
resource "aws_route53_record" "docker_repository_star" {
  zone_id = "Z2EP09JXG7OUE8"
  name    = "*.${var.docker_hostname}"
  type    = "CNAME"
  records = [aws_route53_record.docker_repository.fqdn]
  ttl     = "300"
}

resource "aws_route53_record" "cert_validation" {
  zone_id = "Z2EP09JXG7OUE8"
  name    = tolist(aws_acm_certificate.docker_repository_cert.domain_validation_options).0.resource_record_name
  type    = tolist(aws_acm_certificate.docker_repository_cert.domain_validation_options).0.resource_record_type
  records = [tolist(aws_acm_certificate.docker_repository_cert.domain_validation_options).0.resource_record_value]
  ttl     = 60
}