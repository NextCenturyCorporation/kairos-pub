data "aws_eks_cluster" "perfevalk8s" {
  name = local.perfeval.eks_cluster_name
}

data "aws_eks_cluster_auth" "perfevalk8s" {
  name = local.perfeval.eks_cluster_name
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.perfevalk8s.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.perfevalk8s.certificate_authority.0.data)
  token                  = data.aws_eks_cluster_auth.perfevalk8s.token
  version                = "2.11.0"
}
