resource "aws_instance" "elastic" {

  connection {
    host = coalesce(self.public_ip, self.private_ip)
    type = "ssh"
    user = "ubuntu"

    private_key = file(local.key_pair_file)
  }

  instance_type = var.instance_type
  ami = data.aws_ami.ubuntu.image_id
  iam_instance_profile = local.env.s3_iam_profile

  key_name = local.key_pair_name

  vpc_security_group_ids      = [local.env.sg_developer_access_id, aws_security_group.elisa_access.id]
  subnet_id                   = local.env.vpc.subnets.public[0]
  associate_public_ip_address = true

  root_block_device {
    volume_type = "gp2"
    volume_size = 64
  }

  tags = { 
      Name = var.hostname
  }
}

resource "null_resource" "elastic_upload" {
  connection {
    host        = coalesce(aws_instance.elastic.public_ip, aws_instance.elastic.private_ip)
    type        = "ssh"
    user        = "ubuntu"
    private_key = file(local.key_pair_file)
  }

  depends_on = [
    aws_instance.elastic
  ]

  triggers = {
    beachhead = aws_instance.elastic.public_ip,
    always_run = timestamp()
  }

  provisioner "remote-exec" {
    inline = [
      "sudo rm -rf /home/ubuntu/elastic"
    ]
  }

  provisioner "file" {
    source      = "./sbin"
    destination = "./elastic"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod +x ~/elastic/*.sh",
      "cd ~/elastic; ./install.sh"
    ]
  }

}

resource "null_resource" "elastic_deploy" {
  depends_on = [
    aws_instance.elastic
  ]

  triggers = {
    elastic_id = aws_instance.elastic.id
  }

  provisioner "local-exec" {
    command = "/bin/bash -c \"ansible-playbook -i ${path.module}/../../ansible/inventory ${path.module}/../../ansible/provision_elisa_elastic.yml\""
  }
}

resource "null_resource" "elastic_start" {
  connection {
    host        = coalesce(aws_instance.elastic.public_ip, aws_instance.elastic.private_ip)
    type        = "ssh"
    user        = "ubuntu"
    private_key = file(local.key_pair_file)
  }

  depends_on = [
    aws_instance.elastic,
    null_resource.elastic_deploy
  ]

  triggers = {
    elastic_id = aws_instance.elastic.id
    elastic_upload = null_resource.elastic_upload.id
  }

  provisioner "remote-exec" {
    inline = [
      "cd ~/elastic; docker build -f Kibana.yml -t kibana:elisa ."
    ]
  }

  provisioner "remote-exec" {
    inline = [
      "cd ~/elastic; docker-compose down && docker-compose up -d"
    ]
  }
}