variable "https_port" {
  default = "443"
}

resource "aws_alb_listener" "https" {
  load_balancer_arn = aws_alb.main.id
  port              = var.https_port
  protocol          = "HTTPS"
  certificate_arn   = aws_acm_certificate.validation_external_cert.arn

  depends_on = [
    aws_alb_target_group.main,
    aws_route53_record.cert_validation_external
  ]

  default_action {
    target_group_arn = aws_alb_target_group.main.id
    type             = "forward"
  }
}