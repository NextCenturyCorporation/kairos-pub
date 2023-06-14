resource "aws_lb" "gitlab-lb" {
  name               = "gitlab-lb"
  load_balancer_type = "network"
  internal           = false
  subnets            = [aws_instance.gitlab.subnet_id]
}

resource "aws_lb_target_group" "gitlab_https" {
  name     = "gitlab-http-target"
  port     = 80
  protocol = "TCP"
  vpc_id   = local.vpc_id
}

resource "aws_lb_target_group_attachment" "gitlab_https_attachment" {
  target_group_arn = aws_lb_target_group.gitlab_https.arn
  target_id        = aws_instance.gitlab.id
  port             = 80
}

resource "aws_lb_listener" "gitlab_https_listener" {
  load_balancer_arn = aws_lb.gitlab-lb.arn
  port              = "443"
  protocol          = "TLS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = aws_acm_certificate.gitlab-cert.arn

  default_action {
    target_group_arn = aws_lb_target_group.gitlab_https.arn
    type             = "forward"
  }
}

resource "aws_lb_target_group" "gitlab_ssh" {
  name     = "gitlab-ssh-target"
  port     = 22
  protocol = "TCP"
  vpc_id   = local.vpc_id
}

resource "aws_lb_target_group_attachment" "gitlab_ssh_attachment" {
  target_group_arn = aws_lb_target_group.gitlab_ssh.arn
  target_id        = aws_instance.gitlab.id
  port             = 22
}

resource "aws_lb_listener" "gitlab_ssh_listener" {
  load_balancer_arn = aws_lb.gitlab-lb.arn
  port              = "22"
  protocol          = "TCP"

  default_action {
    target_group_arn = aws_lb_target_group.gitlab_ssh.arn
    type             = "forward"
  }
}
