# resource "aws_instance" "validation_internal" {
#   connection {
#     host = coalesce(self.public_ip, self.private_ip)
#     type = "ssh"
#     user = "ubuntu"

#     private_key = file(local.aws_key_pair_file)
#   }

#   instance_type        = var.instance_type_internal
#   ami                  = var.aws_ami
#   availability_zone    = var.aws_availability_zone
#   iam_instance_profile = local.instance_profile

#   key_name = local.aws_key_pair_name

#   vpc_security_group_ids      = [data.terraform_remote_state.environment.outputs.sg_developer_access_id]
#   subnet_id                   = data.terraform_remote_state.environment.outputs.public_1a_id
#   associate_public_ip_address = true

#   tags = {
#     Name        = "Validation_Internal"
#     AccessClass = "internal"
#   }

#   provisioner "file" {
#     content = data.template_file.healthcheck_internal.rendered
#     destination = "/home/ubuntu/healthcheck.sh"
#   }

#   provisioner "remote-exec" {
#     inline = [
#       "sudo apt update",
#       "sudo chmod +x ~/*.sh"
#     ]
#   }
# }

# data "template_file" "healthcheck_internal" {
#   template = file("${path.module}/templates/healthcheck.sh.template")
#   vars = {
#     httpUrl = "http://internal.validation.kairos.nextcentury.com/#/KSF/validateKsfRequest"
#     container_name = "validation"
#   }
# }