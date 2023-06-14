
output "program_name" {
  value = var.program_name
}
output "environment" {
  value = var.environment
}
output "aws_region" {
  value = var.aws_region
}

output "docker" {
  value = {
    username = "YWRtaW4K"
    password = "S2FpcjBzX3B3ZDMzMTIyCg=="
  }
}

output beachhead_profile_id {
  value = aws_iam_instance_profile.k8s_beachhead_access.id
}
output beachhead_role_arn {
  value = aws_iam_role.k8s_beachhead_access.arn
}

output roles {
  value = {
    k8s_beachhead_access = aws_iam_role.k8s_beachhead_access,
    k8s_cluster_access   = aws_iam_role.k8s_cluster_access
  }
}

output security_group_id {
  value = aws_security_group.experiment_runtime.id
}

output "vpc" {
  value = local.vpc
}

output "machine_key_pair_name" {
  value = local.admin_key_pair_name
}

output "perfevalcluster_name" {
  value = local.perfevalcluster_name
}
