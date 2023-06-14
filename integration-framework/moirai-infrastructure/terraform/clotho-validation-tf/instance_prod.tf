resource "aws_instance" "validation_external" {
  connection {
    host = coalesce(self.public_ip, self.private_ip)
    type = "ssh"
    user = "ubuntu"

    private_key = file(local.aws_key_pair_file)
  }

  instance_type = var.instance_type_external
  ami           = var.aws_ami
  #   availability_zone    = var.aws_availability_zone
  iam_instance_profile = local.instance_profile

  key_name = local.aws_key_pair_name

  vpc_security_group_ids = concat([local.env.sg_external_access_id, local.env.sg_developer_access_id])
  
  subnet_id              = local.vpc.subnets.public[1]

  associate_public_ip_address = true

  tags = {
    Name        = "Validation_External"
    AccessClass = "external"
    "Patch Group" = "Ubuntu"
  }

  provisioner "file" {
    content     = data.template_file.healthcheck_external.rendered
    destination = "/home/ubuntu/healthcheck.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "sudo apt update",
      "sudo chmod +x ~/*.sh"
    ]
  }
}

data "template_file" "healthcheck_external" {
  template = file("${path.module}/templates/healthcheck.sh.template")
  vars = {
    httpUrl        = "https://validation.kairos.nextcentury.com/#/KSF/validateKsfRequest"
    container_name = "validation"
  }
}
