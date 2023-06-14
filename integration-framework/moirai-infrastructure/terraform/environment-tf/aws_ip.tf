resource "aws_eip" "gitlab_ip" {
  vpc      = true
  tags = {
    description = "Static ip for gitlab-runner instance",
    Name        = "gitlab-runner"
  }
}