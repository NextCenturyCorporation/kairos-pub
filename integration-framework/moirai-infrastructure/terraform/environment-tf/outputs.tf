output "vpc_id" {
  value = module.vpc.vpc_id
}

output "vpc" {
  value = {
    id   = module.vpc.vpc_id
    name = module.vpc.name
    subnets = {
      private = module.vpc.private_subnets
      public  = module.vpc.public_subnets
      all     = concat(module.vpc.public_subnets, module.vpc.private_subnets)
    }
  }
}

output "s3_iam_profile" {
  value = aws_iam_instance_profile.s3_access.id
}

output "sg_developer_access_id" {
  value = aws_security_group.developer_access.id
}

output "sg_external_access_id" {
  value = aws_security_group.external_access.id
}

output "clotho_iam_profile" {
  value = aws_iam_instance_profile.clotho_access.id
}

output "approved_cidrs" {
  value = local.approved_cidrs
}

output "domain_name" {
  value = "kairos.nextcentury.com"
}

output "project_name" {
  value = var.project_name
}

output "admin_key_pair_name" {
  value = "${var.project_name}-admin"
}

output "machine_key_pair_name" {
  value = "moirai-machine"
}

output "aws_region" {
  value = "us-east-1"
}

output "route_53_zone_id" {
  value = "Z2EP09JXG7OUE8"
}

output "aws_zeus_secret" {
  sensitive = true
  value = aws_iam_access_key.user.secret
}

output "aws_zeus_access_key" {
  value = aws_iam_access_key.user.id
}

output "beach_heads_policy_id" {
  value = aws_iam_instance_profile.beach_head_access.id
}

output "gitlab_runner_ip" {
  value = aws_eip.gitlab_ip
}