resource "aws_ec2_tag" "shared_subnet_tag" {
  for_each    = toset(concat(local.env.vpc.subnets.private, local.env.vpc.subnets.public))
  resource_id = each.value
  key         = "kubernetes.io/cluster/${local.perfevalcluster_name}"
  value       = "shared"
}

resource "aws_ec2_tag" "private_subnet_tag" {
  for_each    = toset(local.env.vpc.subnets.private)
  resource_id = each.value
  key         = "kubernetes.io/role/internal-elb"
  value       = "1"
}

resource "aws_ec2_tag" "public_subnet_tag" {
  for_each    = toset(local.env.vpc.subnets.public)
  resource_id = each.value
  key         = "kubernetes.io/role/elb"
  value       = "1"
}
