data "aws_vpc" "vpc" {
  id = local.env.vpc.id
}

data "aws_subnets" "private" {
  filter {
    name   = "vpc-id"
    values = [local.env.vpc.id]
  }
  filter {
    name   = "tag:kubernetes.io/role/internal-elb"
    values = ["1"]
  }
}

resource "aws_eks_node_group" "framework" {
  node_group_name = "${var.experiment}-frameworkng"

  cluster_name = local.perfeval.eks_cluster_name

  scaling_config {
    desired_size = var.cpu_nodesmin
    max_size     = var.cpu_nodesmax
    min_size     = var.cpu_nodesmin
  }

  node_role_arn = local.perfeval.eks_node_role_arn

  subnet_ids = data.aws_subnets.private.ids

  disk_size      = var.cpu_nodevolsize
  instance_types = [var.cpu_nodetype]

  labels = {
    "kairosnodetype" = "${var.experiment}-framework"
  }

  tags = {
    "Name" = "${local.perfeval.eks_cluster_name}-framework",
  }

  lifecycle {
    create_before_destroy = true
    ignore_changes        = [scaling_config.0.desired_size]
  }
}

resource "aws_eks_node_group" "workerng" {
  node_group_name = "${var.experiment}-workerng"

  cluster_name = local.perfeval.eks_cluster_name
  ami_type     = "AL2_x86_64_GPU"

  scaling_config {
    desired_size = var.gpu_nodesmin
    max_size     = var.gpu_nodesmax
    min_size     = var.gpu_nodesmin
  }

  node_role_arn = local.perfeval.eks_node_role_arn

  subnet_ids = data.aws_subnets.private.ids

  disk_size      = var.gpu_nodevolsize
  instance_types = [var.gpu_nodetype]

  labels = {
    "kairosnodetype" = "${var.experiment}-worker"
  }

  tags = {
    "Name" = "${local.perfeval.eks_cluster_name}-worker",
  }

  lifecycle {
    create_before_destroy = true
    ignore_changes        = [scaling_config.0.desired_size]
  }
}
