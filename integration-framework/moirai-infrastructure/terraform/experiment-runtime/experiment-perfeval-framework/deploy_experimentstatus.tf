resource "null_resource" "experimentstatus_deployment" {
  connection {
    host        = local.perfeval.beachhead.public_ip
    type        = "ssh"
    user        = local.perfeval.beachhead.user
    private_key = file(local.machine_key_pair_file)
  }

  depends_on = [
    null_resource.beachhead_upload
  ]

  provisioner "remote-exec" {
    inline = [
      "kubectl delete namespace kairos-experimentstatus",
      "cp /home/ec2-user/deployments/experimentstatus/experimentstatus-service.yaml.template /home/ec2-user/deployments/experimentstatus/experimentstatus-service.yaml",
      "cp /home/ec2-user/deployments/experimentstatus/experimentstatus-deployment.yaml.template /home/ec2-user/deployments/experimentstatus/experimentstatus-deployment.yaml",
      # "cp /home/ec2-user/deployments/experimentstatus/experimentstatus-ingress.yaml.template /home/ec2-user/deployments/experimentstatus/experimentstatus-ingress.yaml",
      "kubectl create namespace kairos-experimentstatus",
      "kubectl apply -f /home/ec2-user/deployments/experimentstatus -n kairos-experimentstatus",
      "source /etc/profile.d/kairos-functions.sh; waitForNamespace kairos-experimentstatus"
    ]
  }
}

resource "aws_route53_record" "experimentstatus" {
  zone_id = "Z2EP09JXG7OUE8"
  name    = local.experimentstatus_hostname
  type    = "CNAME"
  records = [data.kubernetes_service.experimentstatus.status.0.load_balancer.0.ingress.0.hostname]
  ttl     = "300"
}

data "kubernetes_service" "experimentstatus" {
  depends_on = [
    null_resource.experimentstatus_deployment
  ]

  metadata {
    name = "kairos-experimentstatus"
    namespace = "kairos-experimentstatus"
  }
}