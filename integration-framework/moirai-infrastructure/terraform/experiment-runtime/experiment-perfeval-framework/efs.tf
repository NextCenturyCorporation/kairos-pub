resource "aws_efs_file_system" "deploy_efs" {
  creation_token = "efs-${local.env.perfevalcluster_name}-efs"

  tags = {
    Name = "${local.env.perfevalcluster_name}-EFS"
  }
}

resource "aws_efs_mount_target" "vpc_public" {
  for_each = toset(local.env.vpc.subnets.public)

  file_system_id  = aws_efs_file_system.deploy_efs.id
  subnet_id       = each.value
  security_groups = [local.env.security_group_id]
}

resource "null_resource" "deployEfs" {
  connection {
    host        = local.perfeval.beachhead.public_ip
    type        = "ssh"
    user        = local.perfeval.beachhead.user
    private_key = file(local.machine_key_pair_file)
  }

  depends_on = [
    aws_efs_file_system.deploy_efs,
    aws_efs_mount_target.vpc_public
  ]

  # todo https://stackoverflow.com/questions/65966367/whats-the-equivalent-of-kubernetes-apply-k-in-terraform
  provisioner "remote-exec" {
    inline = [
      "kubectl apply -k \"github.com/kubernetes-sigs/aws-efs-csi-driver/deploy/kubernetes/overlays/stable/?ref=release-1.5\""
    ]
  }

  provisioner "remote-exec" {
    inline = [
      "FSPATH=\"/var/kairosfs\"",
      "sudo umount --force $FSPATH",
      "sudo mkdir -pv $FSPATH",
      "sudo umount --force $FSPATH",
      "sleep 30",
      "sudo mount -t efs ${aws_efs_file_system.deploy_efs.id}:/ $FSPATH",
    ]
  }
}
