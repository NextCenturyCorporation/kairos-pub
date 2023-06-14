# - add to user data
# - 'sudo yum -y install epel-release'

data "aws_ami" "amazon_linux" {
  owners      = ["amazon"]
  most_recent = true

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-gp2"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_route53_record" "k8s_beachhead" {
  zone_id = "Z2EP09JXG7OUE8"
  name    = local.control_box_hostname
  type    = "CNAME"
  records = [aws_instance.k8s_beachhead.public_dns]
  ttl     = "300"
}

resource "aws_instance" "k8s_beachhead" {
  connection {
    host = coalesce(self.public_ip, self.private_ip)
    type = "ssh"
    user = var.beachhead_ami_user

    private_key = file(local.admin_key_pair_file)
  }

  instance_type        = var.beachhead_instance_type
  ami                  = data.aws_ami.amazon_linux.id
  iam_instance_profile = local.env.beachhead_profile_id

  key_name = local.admin_key_pair_name

  user_data = templatefile(
    "./user-data-files/beachhead.tpl", {
      pythonversion          = "3"
      yqversion              = "4.4.1"
      javajdk                = "openjdk"
      javaversion            = "11"
      kafkaversion           = "2.12-2.8.0"
      awscliversion          = "v2"
      kubectlversion         = "1.22.6"
      kubectldate            = "2022-03-09"
      helmversion            = "3"
      alb-ingress-controller = "1.1.8"
      komposeversion         = "1.23.0"
      nifi                   = "1.11.4"
    }
  )

  vpc_security_group_ids      = concat([local.env.security_group_id])
  subnet_id                   = local.env.vpc.subnets.public[0]
  associate_public_ip_address = true

  tags = {
    Name = "${local.perfevalcluster_name}-controlBox"
  }

  lifecycle {
    ignore_changes = [
      ami
    ]
  }
}

resource "null_resource" "k8s_beachhead_upload" {
  connection {
    host        = aws_instance.k8s_beachhead.public_ip
    type        = "ssh"
    user        = var.beachhead_ami_user
    private_key = file(local.admin_key_pair_file)
  }

  triggers = {
    always_run = timestamp()
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir /home/ec2-user/infrastructure-config/ || true",
      "mkdir /home/ec2-user/infra-templates/ || true",
      "mkdir /home/ec2-user/scripts/ || true",
      "mkdir /home/ec2-user/k8s-runtimes/ || true",
      "mkdir /home/ec2-user/experiment-config/ || true",
      "rm -f kairos-key.pem"
    ]
  }

  provisioner "file" {
    source      = "${path.module}/sbin/"
    destination = "/home/ec2-user/scripts"
  }

  provisioner "file" {
    source      = "${path.module}/templates/infra-templates/"
    destination = "/home/ec2-user/infra-templates"
  }

  provisioner "file" {
    source      = "${path.module}/k8s-runtimes/"
    destination = "/home/ec2-user/k8s-runtimes"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod +x /home/ec2-user/**/*.sh",
      "sudo cp /home/ec2-user/scripts/kairos-functions.sh /etc/profile.d/kairos-functions.sh"
    ]
  }

  provisioner "file" {
    source      = local.admin_key_pair_file
    destination = "/home/ec2-user/kairos-key.pem"
  }
}

resource "null_resource" "beachhead_ready" {
  depends_on = [
    aws_instance.k8s_beachhead,
    null_resource.k8s_beachhead_upload,
    aws_route53_record.k8s_beachhead
  ]
  provisioner "local-exec" {
    command = "echo =============== BEACH HEAD - complete ==============="
  }
}

output "Connect-Controlbox" {
  value = format(
    "ssh -i %s %s@%s",
    local.admin_key_pair_file,
    var.beachhead_ami_user,
    aws_route53_record.k8s_beachhead.fqdn,
  )
}

output "beachhead" {
  value = {
    public_ip = aws_instance.k8s_beachhead.public_ip
    user      = var.beachhead_ami_user
  }
}
