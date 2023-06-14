resource "aws_instance" "developers_box" {

  connection {
    host = coalesce(self.public_ip, self.private_ip)
    type = "ssh"
    user = "ubuntu"

    private_key = file(local.aws_key_pair_file)
  }

  instance_type        = var.instance_type
  ami                  = data.aws_ami.ubuntu.image_id
  availability_zone    = var.aws_availability_zone
  iam_instance_profile = aws_iam_instance_profile.developers_box_access.id
  ebs_optimized        = var.ebs_optimized[var.instance_type]

  key_name = local.aws_key_pair_name

  vpc_security_group_ids      = concat([local.env.sg_developer_access_id])
  subnet_id                   = local.env.vpc.subnets.public[0]
  associate_public_ip_address = true

  root_block_device {
    volume_type = "gp2"
    volume_size = 32

  }

  count = var.num_instances

  tags = {
    Name        = "Developers-Box"
    AccessClass = "external"
  }

  lifecycle {
    ignore_changes = [
      ami
    ]
  }
  
  provisioner "file" {
    source = "${local.project_root}/../moirai-infrastructure"
    destination = "/home/ubuntu/moirai-infrastructure"
  }

  provisioner "remote-exec" {
    inline = [
      "sudo apt update",
      "sudo apt install dos2unix -y",
      "chmod +x moirai-infrastructure/install/*",
      "chmod +x moirai-infrastructure/ansible/inventory/*",
      "chmod +x moirai-infrastructure/k8s/*",
      "chmod 400 moirai-infrastructure/key-pairs/*.pem",
      "sudo dos2unix moirai-infrastructure/ansible/inventory/*",
      "sudo bash moirai-infrastructure/install/install_terraform.sh",
      "sudo bash moirai-infrastructure/install/install_ansible.sh",
      "sudo sh -c 'echo \"${local.deply_extravars}\" >> /etc/environment'",
      "sudo apt install git-all -y",
      "sudo apt install default-jre -y",
      "sudo apt install default-jdk -y",
      "sudo apt install nodejs -y",
      "sudo apt install npm -y",
      "sudo curl -L \"https://github.com/docker/compose/releases/download/1.27.4/docker-compose-$(uname -s)-$(uname -m)\" -o /usr/local/bin/docker-compose",
      "sudo chmod +x /usr/local/bin/docker-compose"
    ]
  }
  provisioner "remote-exec" {
    inline = [
      "ansible-galaxy install -r ~/moirai-infrastructure/ansible/requirements.yml",
      "ansible-playbook -i ~/moirai-infrastructure/ansible/inventory ~/moirai-infrastructure/ansible/deploy-dev-box.yml",
      "echo All Done remote exec"
    ]
  }
}

output "PublicIp" {
  value = join(",", aws_instance.developers_box.*.public_dns)
}

output "Connect" {
  value = formatlist(
    "ssh -i %s ubuntu@%s",
    local.aws_key_pair_file,
    aws_instance.developers_box.*.public_dns,
  )
}

