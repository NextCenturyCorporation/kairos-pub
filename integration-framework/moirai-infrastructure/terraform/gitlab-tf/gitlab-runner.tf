resource "aws_instance" "gitlab-runner" {
  ami           = data.aws_ami.ubuntu.image_id
  instance_type = var.runner_instance_type
  key_name      = local.key_pair_name

  connection {
    host = coalesce(self.public_ip, self.private_ip)
    type = "ssh"
    user = "ubuntu"

    private_key = file(local.key_pair_file)
  }

  depends_on = [aws_instance.gitlab]

  user_data = templatefile(
    "./user-data-files/gitlab-runner.tpl", {
      REGISTER-GITLAB-RUNNER-SCRIPT = base64encode(data.template_file.register-gitlab-runner-script.rendered),
      GITLAB-HOSTNAME               = local.gitlab_hostname,
      GITLAB-IP                     = aws_instance.gitlab.private_ip
    }
  )

  iam_instance_profile   = aws_iam_instance_profile.gitlab_runner_s3.name
  vpc_security_group_ids = concat([local.beach_head_sg, aws_security_group.gitlab-services_securitygroup.id])
  subnet_id              = local.subnet_id

  root_block_device {
    volume_type           = "gp2"
    volume_size           = var.runner_volume_size
    delete_on_termination = true
  }

  tags = {
    Name      = "GitlabRunner"
    ProjectID = local.project_name
  }

  lifecycle {
    ignore_changes = [
      ami
    ]
  }

  provisioner "remote-exec" {
    inline = [
      "until [ -f /tmp/ready ]; do sleep 10; done"
    ]
  }
}

resource "aws_eip_association" "eip_assoc" {
  instance_id   = aws_instance.gitlab-runner.id
  allocation_id = local.env.gitlab_runner_ip.id
}

resource "null_resource" "gitlab-runner" {

  depends_on = [
    aws_instance.gitlab-runner,
    null_resource.gitlab-registation-catch
  ]

  triggers = {
    runner     = aws_instance.gitlab-runner.public_ip
    registered = null_resource.gitlab-registation-catch.id
  }

  provisioner "local-exec" {
    command = "ansible-playbook -i ${local.ansible_dir}/inventory ${local.ansible_dir}/gitlab-runner-init.yml --extra-vars \"ansible_ssh_private_key_file=${local.key_pair_file}\""
  }
}
