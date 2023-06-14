output "eks_cluster_name" {
  value = local.perfevalcluster_name
}

output "eks_cluster_arn" {
  value = data.aws_eks_cluster.perfevalk8s.arn
}

output "eks_cluster_role_arn" {
  value = data.aws_eks_cluster.perfevalk8s.role_arn
}

output "eks_node_role_arn" {
  value = module.perfevalk8s.eks_managed_node_groups["framework"].iam_role_arn
}
