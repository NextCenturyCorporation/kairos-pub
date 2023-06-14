resource "null_resource" "gitlab-registration" {
  depends_on = [
    aws_instance.gitlab,
    aws_instance.gitlab-runner
  ]
  triggers = {
    gitlab = aws_instance.gitlab.public_ip
    runner = aws_instance.gitlab.arn
  }
  provisioner "local-exec" {
    command = "echo Registering runner"
  }
}

resource "null_resource" "gitlab-registation-serve" {

  depends_on = [
    null_resource.gitlab-registration
  ]

  triggers = {
    registration = null_resource.gitlab-registration.id
  }

  provisioner "remote-exec" {
    connection {
      host        = aws_instance.gitlab.public_dns
      type        = "ssh"
      user        = "ubuntu"
      private_key = file(local.key_pair_file)
    }
    inline = [
      "sudo /root/serve-token.sh"
    ]
  }
}

resource "null_resource" "gitlab-registation-catch" {

  depends_on = [
    null_resource.gitlab-registration
  ]

  triggers = {
    registration = null_resource.gitlab-registration.id
  }

  provisioner "remote-exec" {
    connection {
      host        = aws_instance.gitlab-runner.public_dns
      type        = "ssh"
      user        = "ubuntu"
      private_key = file(local.key_pair_file)
    }
    inline = ["sudo /root/register-gitlab-runner.sh"]
  }
}
