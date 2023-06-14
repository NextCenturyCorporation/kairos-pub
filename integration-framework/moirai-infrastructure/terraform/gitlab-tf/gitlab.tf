resource "aws_instance" "gitlab" {
  ami           = var.instance_ami
  instance_type = var.gitlab_instance_type
  key_name      = local.key_pair_name

  connection {
    host = coalesce(self.public_ip, self.private_ip)
    type = "ssh"
    user = "ubuntu"

    private_key = file(local.key_pair_file)
  }

  user_data = templatefile(
    "./user-data-files/gitlab.tpl", {
      GITLAB-HOSTNAME         = local.gitlab_hostname
      GITLAB-VERSION-SCRIPT   = data.local_file.gitlab-version-script.content_base64
      GITLAB-INIT-SCRIPT      = data.local_file.gitlab-init-script.content_base64
      GITLAB-RB-APPEND        = data.local_file.gitlab-rb-append.content_base64
      GITLAB-BACKUP-CRONJOB   = data.local_file.gitlab-backup-cronjob.content_base64
      GITLAB-TOKEN-SCRIPT     = data.local_file.gitlab-token-script.content_base64
      GITLAB-BACKUP-S3-BUCKET = local.gitlab_backup_s3_bucket
      GITLAB-ETC-S3-BUCKET    = local.gitlab_etc_s3_bucket
      GITLAB-CLEANUP-SCRIPT   = data.local_file.gitlab-cleanup-script.content_base64
      GITLAB-CLEANUP-CRONJOB  = data.local_file.gitlab-cleanup-cronjob.content_base64
      AWS-REGION              = local.aws_region
    }
  )

  iam_instance_profile   = aws_iam_instance_profile.gitlab-backup.name
  vpc_security_group_ids = concat([local.beach_head_sg, aws_security_group.gitlab-services_securitygroup.id])
  subnet_id              = local.subnet_id

  root_block_device {
    volume_type           = "gp2"
    volume_size           = var.gitlab_volume_size
    delete_on_termination = true
  }

  tags = {
    Name      = "Gitlab"
    ProjectID = local.project_name
  }

  lifecycle {
    ignore_changes = [
      ami
    ]
  }
  
  provisioner "remote-exec" {
    inline = [
      "until [ -f /tmp/ready ]; do sleep 20; done"
    ]
  }
}

data "aws_instance" "foo" {
  filter {
    name   = "tag:Name"
    values = ["Developers-Box"]
  }
}

resource "null_resource" "gitlab-devbox-connection" {
  connection {
    host        = coalesce(data.aws_instance.foo.public_ip, data.aws_instance.foo.private_ip)
    type        = "ssh"
    user        = "ubuntu"
    private_key = file(local.key_pair_file)
  }

  depends_on = [aws_instance.gitlab]

  triggers = {
    watch_gitlab = aws_instance.gitlab.private_ip
  }

  provisioner "remote-exec" {
    inline = [
      "cat /etc/hosts | grep -v 'gitlab.kairos.nextcentury.com' > /tmp/hosts",
      "echo ${aws_instance.gitlab.private_ip} gitlab.kairos.nextcentury.com >> /tmp/hosts",
      "chmod 644 /tmp/hosts",
      "sudo mv /tmp/hosts /etc/hosts"
    ]
  }
}
