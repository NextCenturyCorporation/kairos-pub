resource "null_resource" "provision_ansible" {
  depends_on = [
    aws_instance.validation_external,
    # aws_instance.validation_internal,
    # aws_instance.validation_dev,
  ]

  triggers = {
    prod_id = aws_instance.validation_external.id
    # internal_id=aws_instance.validation_internal.id
    # dev_id = aws_instance.validation_dev.id
  }

  provisioner "local-exec" {
    command = "ansible-playbook -i ${path.module}/../../ansible/inventory ${path.module}/../../ansible/provision_validation.yml"
  }
}
