resource "aws_instance" "docker_repository" {
  connection {
    host = coalesce(self.public_ip, self.private_ip)
    type = "ssh"
    user = "ubuntu"

    private_key = file(local.key_pair_file)
  }

  instance_type        = var.instance_type
  ami                  = data.aws_ami.ubuntu.image_id
  availability_zone    = var.aws_availability_zone
  iam_instance_profile = aws_iam_instance_profile.docker_repository_access_profile.id

  key_name = local.key_pair_name

  vpc_security_group_ids      =   concat([local.env.sg_external_access_id, local.env.sg_developer_access_id, aws_security_group.docker_registry_access.id])

  subnet_id                   = local.vpc.subnets.public[0]
  associate_public_ip_address = true

  root_block_device {
    volume_type = "gp2"
    volume_size = 256
  }

  lifecycle {
    ignore_changes = [
      ami
    ]
  }

  tags = {
    Name        = "Docker_Repository"
    AccessClass = "external"
  }

  provisioner "file" {
    source = "./configuration/config.yml"
    destination = "/home/ubuntu/config.yml"
  }

  provisioner "file" {
    source = "${local.project_root}/../moirai-infrastructure"
    destination = "/home/ubuntu/moirai-infrastructure"
  }

  provisioner "remote-exec" {
    inline = [
      "sudo apt update",
      "sudo apt install dos2unix -y",
      "sudo apt-get install apache2-utils -y",
      "chmod +x /home/ubuntu/*.sh",
      "chmod +x moirai-infrastructure/install/*",
      "chmod +x moirai-infrastructure/ansible/inventory/*",
      "chmod 400 moirai-infrastructure/key-pairs/*.pem",
      "sudo dos2unix moirai-infrastructure/ansible/inventory/*",
      "sudo bash moirai-infrastructure/install/install_ansible.sh",
      "echo All Done remote exec",
      "sudo apt install awscli -y",
      "pip3 install --upgrade pip",
      "pip3 install --upgrade awscli",
    ]
  }
}

resource "null_resource" "docker_upload" {
  connection {
    host        = coalesce(aws_instance.docker_repository.public_ip, aws_instance.docker_repository.private_ip)
    type        = "ssh"
    user        = "ubuntu"
    private_key = file(local.key_pair_file)
  }

  depends_on = [
    aws_instance.docker_repository
  ]

  triggers = {
    docker_repository = aws_instance.docker_repository.public_ip
  }
  provisioner "remote-exec" {
    inline = [
      "sudo rm -rf /home/ubuntu/moirai-infrastructure"
    ]
  }

  provisioner "file" {
    source      = "${local.project_root}/../moirai-infrastructure"
    destination = "/home/ubuntu/moirai-infrastructure"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod +x moirai-infrastructure/install/*",
      "chmod +x moirai-infrastructure/ansible/inventory/*",
      "chmod 400 moirai-infrastructure/key-pairs/*.pem",
      "sudo dos2unix moirai-infrastructure/ansible/inventory/*",
    ]
  }
}

resource "null_resource" "cert-setup" {
  depends_on = [aws_route53_record.cert_validation]

  connection {
    host = coalesce(aws_instance.docker_repository.public_ip, aws_instance.docker_repository.private_ip)
    type = "ssh"
    user = "ubuntu"
    private_key = file(local.key_pair_file)
  }

  provisioner "remote-exec" {
    inline = [
      "ansible-galaxy install -r ~/moirai-infrastructure/ansible/requirements.yml",
      "ansible-playbook -i ~/moirai-infrastructure/ansible/inventory ~/moirai-infrastructure/ansible/deploy-docker-repository.yml",
    ]
  }
}