data "template_file" "kafdrop_deployment" {
  template = file("${path.module}/deployments/kafdrop/kafdrop-deployment.yaml.template")
  vars = {
    broker_endpoint = local.env.kafka.endpoints[0]
  }
}

resource "null_resource" "kafdrop_deployment" { # create-kafka-cluster.deployKafkaBrowser() && create-k8s-cluster.deployKafkaBrowser()
  connection {
    host        = local.perfeval.beachhead.public_ip
    type        = "ssh"
    user        = local.perfeval.beachhead.user
    private_key = file(local.machine_key_pair_file)
  }

  depends_on = [
    null_resource.beachhead_upload
  ]

  provisioner "file" {
    content     = data.template_file.kafdrop_deployment.rendered
    destination = "/home/${local.perfeval.beachhead.user}/deployments/kafdrop/kafdrop-deployment.yaml"
  }

  provisioner "remote-exec" {
    inline = [
      "kubectl delete -f ~/deployments/kafdrop || true",
      "kubectl apply -f ~/deployments/kafdrop"
    ]
  }
}
