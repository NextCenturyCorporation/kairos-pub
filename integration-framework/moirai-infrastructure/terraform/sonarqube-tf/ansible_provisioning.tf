resource "null_resource" "provision_ansible" {
  depends_on = [
    aws_instance.sonarqube_instance,
  ]

  triggers = {
    prod_id = aws_instance.sonarqube_instance.id
  }

  provisioner "local-exec" {
    command = "ansible-playbook -i ${path.module}/../../ansible/inventory ${path.module}/../../ansible/provision-sonarqube.yml"
  }
}
