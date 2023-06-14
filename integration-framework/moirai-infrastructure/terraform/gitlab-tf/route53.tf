resource "aws_route53_record" "gitlab" {
  zone_id = local.route_53_zone_id
  name    = local.gitlab_hostname
  type    = "CNAME"
  ttl     = "300"
  records = [aws_lb.gitlab-lb.dns_name]
}

resource "aws_route53_record" "cert_validation" {
  name    = tolist(aws_acm_certificate.gitlab-cert.domain_validation_options).0.resource_record_name
  type    = tolist(aws_acm_certificate.gitlab-cert.domain_validation_options).0.resource_record_type
  zone_id = local.route_53_zone_id
  records = [tolist(aws_acm_certificate.gitlab-cert.domain_validation_options).0.resource_record_value]
  ttl     = 60
}

resource "aws_route53_record" "gitlab_runner" {
  zone_id = local.route_53_zone_id
  name    = local.runner_hostname
  type    = "CNAME"
  ttl     = "300"
  records = [aws_instance.gitlab-runner.public_dns]
}

resource "aws_route53_record" "runner_cert_validation" {
  name    = tolist(aws_acm_certificate.gitlab-runner-cert.domain_validation_options).0.resource_record_name
  type    = tolist(aws_acm_certificate.gitlab-runner-cert.domain_validation_options).0.resource_record_type
  zone_id = local.route_53_zone_id
  records = [tolist(aws_acm_certificate.gitlab-runner-cert.domain_validation_options).0.resource_record_value]
  ttl     = 60
}