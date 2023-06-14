resource "aws_instance" "sonarqube_instance" {
  connection {
    host = coalesce(self.public_ip, self.private_ip)
    type = "ssh"
    user = "ubuntu"

    private_key = file(var.aws_key_file)
  }

  instance_type = var.instance_type
  ami = var.instance_ami
  availability_zone = var.aws_availability_zone
  iam_instance_profile = aws_iam_instance_profile.sonarqube_access.name

  key_name = local.key_pair_name

  vpc_security_group_ids      = concat([local.env.sg_developer_access_id])
  subnet_id                   = local.subnet_id
  associate_public_ip_address = true

  root_block_device {
    volume_type = "gp2"
    volume_size = 1000
  }

  tags = { 
      Name = "SonarQube"
      AccessClass = "external"
      "Patch Group" = "Debian"
  }
}

#output "# Public Ip" { value = aws_instance.sonarqube_instance.public_dns }
output "Public_Ip" {value = join(",",aws_instance.sonarqube_instance.*.public_dns)}
output "Connect" {value=formatlist("ssh -i %s ubuntu@%s",var.aws_key_file,aws_instance.sonarqube_instance.*.public_dns)}