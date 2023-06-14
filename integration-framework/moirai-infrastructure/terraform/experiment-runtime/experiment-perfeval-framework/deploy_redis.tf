resource "null_resource" "redis_deployment" {
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
      "kubectl -n kairos-redis delete pod,sts --all",
      "kubectl delete namespace kairos-redis",
      "sleep 5",
      "echo 'Deploying redis to k8s....'",
      "kubectl create namespace kairos-redis",
      "kubectl apply -f ~/deployments/redis-cluster -n kairos-redis",
      "kubectl get deployment,sts -o name -n kairos-redis | xargs kubectl rollout status -n kairos-redis",
      
      "yesarg=\"echo yes\"",
      "PODS=$(kubectl get pods -n kairos-redis -l app=redis-cluster -o jsonpath='{range.items[*]}{.status.podIP}:6379 ' | sed 's/ :6379 //')",
      "clustercmd=\"$yesarg | redis-cli --cluster create --cluster-replicas 1 $PODS\"",
      "echo Creating cluster - \"cluster cmd: $${clustercmd}\"",
      "echo \"kubectl exec -it redis-cluster-0 -n kairos-redis -- /bin/sh -c \"$${clustercmd}\"  \"",
      "kubectl exec -it redis-cluster-0 -n kairos-redis -- /bin/sh -c \"$${clustercmd}\"  ",
      "sleep 3",
      "echo ====================CHECKING REDIS CLUSTER ===================================",
      "kubectl exec -it redis-cluster-0 -n kairos-redis -- redis-cli cluster info",
      "kubectl exec -it redis-cluster-0 -n kairos-redis -- redis-cli role",
      "kubectl exec -it redis-cluster-1 -n kairos-redis -- redis-cli role",
      "kubectl exec -it redis-cluster-2 -n kairos-redis -- redis-cli role",
      "kubectl exec -it redis-cluster-3 -n kairos-redis -- redis-cli role",
      "kubectl exec -it redis-cluster-4 -n kairos-redis -- redis-cli role",
      "kubectl exec -it redis-cluster-5 -n kairos-redis -- redis-cli role",
      "echo ==============================================================================",
    ]
  }
}
