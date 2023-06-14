resource "aws_acm_certificate" "validation_external_cert" {
  domain_name       = aws_route53_record.validation_external.fqdn
  validation_method = "DNS"

  tags = {
    Environment = "test"
  }

  lifecycle {
    create_before_destroy = true
  }
}

# resource "aws_acm_certificate" "validation_dev_cert" {
#   domain_name       = aws_route53_record.validation_dev.fqdn
#   validation_method = "DNS"

#   tags = {
#     Environment = "test"
#   }

#   lifecycle {
#     create_before_destroy = true
#   }
# }

# resource "aws_acm_certificate" "validation_internal_cert" {
#   domain_name       = aws_route53_record.validation_internal.fqdn
#   validation_method = "DNS"

#   tags = {
#     Environment = "test"
#   }

#   lifecycle {
#     create_before_destroy = true
#   }
# }
