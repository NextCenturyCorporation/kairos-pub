# resource "null_resource" "k8s_dashboard_deployment" {
#   connection {
#     host        = aws_instance.k8s_beachhead.public_ip
#     type        = "ssh"
#     user        = var.beachhead_ami_user
#     private_key = file(local.machine_key_pair_file)
#   }

#   depends_on = [
#     module.perfevalk8s,
#     null_resource.perfevalk8s_configk8sCluster,
#     aws_security_group_rule.perfevalk8s_to_default,
#     null_resource.k8s_beachhead_upload
#   ]

#   provisioner "remote-exec" {
#     inline = [
#       "kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.3.6/components.yaml",
#       "kubectl get deployment metrics-server -n kube-system",
#       "kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.0-beta8/aio/deploy/recommended.yaml",
#       "kubectl apply -f /home/ec2-user/infra-templates/eks-admin-service-account.yaml"
#     ]
#   }
# }
