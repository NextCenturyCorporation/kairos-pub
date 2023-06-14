resource "aws_instance" "beach_head" {
  depends_on = [aws_ecr_repository.genesis_ecr]

  connection {
    host = coalesce(self.public_ip, self.private_ip)
    type = "ssh"
    user = "ubuntu"

    private_key = file(local.key_pair_file)
  }

  instance_type = var.instance_type
  ami           = data.aws_ami.ubuntu.image_id
  # availability_zone    = var.aws_availability_zone
  iam_instance_profile = local.env.beach_heads_policy_id

  key_name = local.key_pair_name

  vpc_security_group_ids      = concat([local.env.sg_external_access_id, local.env.sg_developer_access_id])
  subnet_id                   = local.env.vpc.subnets.public[0]
  associate_public_ip_address = true

  root_block_device {
    volume_type = "gp2"
    volume_size = 32
  }

  tags = {
    Name                    = "BeachHead"
    AccessClass             = "external"
    ExperimentRuntimeAccess = "true"
  }

  lifecycle {
    ignore_changes = [
      ami
    ]
  }

  provisioner "local-exec" {
    command = "ansible-playbook -i ${local.ansible_dir}/inventory ${local.ansible_dir}/beachhead-setup-ecr.yml"
  }

  provisioner "file" {
    source      = "${local.project_root}/../moirai-infrastructure"
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
      "sudo sh -c 'echo \"${local.deploy_zeus_extravars}\" >> /etc/environment'",
      "ansible-playbook -i ~/moirai-infrastructure/ansible/inventory ~/moirai-infrastructure/ansible/provision-zeus.yml",
      "echo All Done remote exec"
    ]
  }
}

resource "null_resource" "beachhead_auth" {
  connection {
    host        = coalesce(aws_instance.beach_head.public_ip, aws_instance.beach_head.private_ip)
    type        = "ssh"
    user        = "ubuntu"
    private_key = file(local.key_pair_file)
  }

  depends_on = [
    aws_instance.beach_head
  ]

  triggers = {
    beachhead = aws_instance.beach_head.public_ip
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir ~/.aws/",

      "echo \"[default]\" > ~/.aws/config",
      "echo \"region = ${local.aws_region}\" >> ~/.aws/config",
      "echo \"output = json\" >> ~/.aws/config",
      "echo \"[kairos]\" >> ~/.aws/config",
      "echo \"region = ${local.aws_region}\" >> ~/.aws/config",
      "echo \"output = json\" >> ~/.aws/config",

      "echo \"[kairos]\" > ~/.aws/credentials",
      "echo \"aws_access_key_id = ${local.env.aws_zeus_access_key}\" >> ~/.aws/credentials",
      "echo \"aws_secret_access_key = ${local.env.aws_zeus_secret}\" >> ~/.aws/credentials",
      "echo \"[default]\" >> ~/.aws/credentials",
      "echo \"aws_access_key_id = ${local.env.aws_zeus_access_key}\" >> ~/.aws/credentials",
      "echo \"aws_secret_access_key = ${local.env.aws_zeus_secret}\" >> ~/.aws/credentials",

      "sudo sed -i 's|aws_access_key=.*|aws_access_key=${local.env.aws_zeus_access_key}|g' /etc/environment ",
      "sudo sed -i 's|aws_secret_key=.*|aws_secret_key=${local.env.aws_zeus_secret}|g' /etc/environment "
    ]
  }
}

resource "null_resource" "beachhead_upload" {
  connection {
    host        = coalesce(aws_instance.beach_head.public_ip, aws_instance.beach_head.private_ip)
    type        = "ssh"
    user        = "ubuntu"
    private_key = file(local.key_pair_file)
  }

  depends_on = [
    aws_instance.beach_head
  ]

  triggers = {
    beachhead = aws_instance.beach_head.public_ip
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
      "chmod +x moirai-infrastructure/k8s/*",
      "chmod 400 moirai-infrastructure/key-pairs/*.pem",
      "sudo dos2unix moirai-infrastructure/ansible/inventory/*",
    ]
  }
}

resource "null_resource" "beachhead_deploy" {
  connection {
    host        = coalesce(aws_instance.beach_head.public_ip, aws_instance.beach_head.private_ip)
    type        = "ssh"
    user        = "ubuntu"
    private_key = file(local.key_pair_file)
  }

  depends_on = [
    aws_instance.beach_head,
    null_resource.beachhead_upload
  ]

  triggers = {
    beachhead = aws_instance.beach_head.public_ip
  }

  provisioner "remote-exec" {
    inline = [
      "ansible-playbook -i ~/moirai-infrastructure/ansible/inventory ~/moirai-infrastructure/ansible/deploy-zeus-dockers.yml"
    ]
  }
}
