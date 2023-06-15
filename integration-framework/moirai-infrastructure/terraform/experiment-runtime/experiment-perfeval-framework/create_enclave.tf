
data "template_file" "create_enclave" {
  template = file("${path.module}/templates/create-enclave-input.json.template")
  vars = {
    efsId                       = aws_efs_file_system.deploy_efs.id,
    region                      = local.env.aws_region,
    perfevalclustername         = local.perfeval.eks_cluster_name,
    perfevalclusterarn          = local.perfeval.eks_cluster_arn,
    submissioningestclustername = "local.submissioncluster_name",
    submissioningestclusterarn  = "data.aws_eks_cluster.submission_ingest.arn",
    mskclusterarn               = local.env.kafka.arn,
    kafkabrokers                = join(",", local.env.kafka.endpoints),
    awsaccesskey                = local.aws_access_key,
    awssecretaccesskey          = local.aws_secret_key,
    nifi_dnsname                = "",
    # nifi_dnsname                = aws_route53_record.nifi_standalone.fqdn,
    nifi_port = 8080
  }
}

resource "null_resource" "create_enclave" {
  connection {
    host        = local.perfeval.beachhead.public_ip
    type        = "ssh"
    user        = local.perfeval.beachhead.user
    private_key = file(local.machine_key_pair_file)
  }

  depends_on = [
    aws_efs_file_system.deploy_efs
  ]

  triggers = {
    value = data.template_file.create_enclave.rendered
  }

  provisioner "file" {
    content     = data.template_file.create_enclave.rendered
    destination = "/home/ec2-user/infrastructure-config/enclave.config"
  }
}
