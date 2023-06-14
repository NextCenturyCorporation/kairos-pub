data "aws_eks_cluster" "perfevalk8s" {
  name = module.perfevalk8s.cluster_id
}

data "aws_eks_cluster_auth" "perfevalk8s" {
  name = module.perfevalk8s.cluster_id
}

data "aws_iam_group" "admin_users" {
  group_name = "Admins"
}

data "aws_iam_group" "hippodrome_users" {
  group_name = "Hippodrome"
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.perfevalk8s.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.perfevalk8s.certificate_authority.0.data)
  token                  = data.aws_eks_cluster_auth.perfevalk8s.token
}

locals {
  perfevalcluster_name = local.env.perfevalcluster_name
  k8s_users = concat(data.aws_iam_group.admin_users.users, data.aws_iam_group.hippodrome_users.users)
}

resource "null_resource" "pre_perfevalk8s" {
  for_each = {
    "${local.perfevalcluster_name}" = "delete"
  }


  provisioner "local-exec" {
    when    = destroy
    command = "aws logs delete-log-group --log-group-name /aws/eks/${each.key}/cluster || echo no group found"
  }
}

module "perfevalk8s" {
  source          = "terraform-aws-modules/eks/aws"
  version         = "18.20.5"
  cluster_name    = local.perfevalcluster_name
  cluster_version = 1.22
  subnet_ids      = local.env.vpc.subnets.private
  vpc_id          = local.env.vpc.id

  create_cluster_security_group = false
  cluster_security_group_id     = local.env.security_group_id
  create_node_security_group    = false
  node_security_group_id        = local.env.security_group_id

  manage_aws_auth_configmap = true
  aws_auth_roles = [
    {
      rolearn  = local.env.beachhead_role_arn
      username = "beachhead_access"
      groups   = ["system:masters"]
    }
  ]

  aws_auth_users = [
    for user in local.k8s_users : {
      userarn  = user.arn
      username = user.user_name
      groups   = ["system:masters"]
    }
  ]

  eks_managed_node_groups = {
    framework = {
      name                   = "framework"
      use_name_prefix        = false
      instance_types         = ["t3a.medium"]
      disk_size              = 100
      desired_size           = 1
      min_size               = 1
      max_size               = 1
      create_launch_template = false
      launch_template_name   = ""
      iam_role_additional_policies = ["arn:aws:iam::aws:policy/AmazonS3FullAccess"]

      additional_tags = {
        Name = "${local.perfevalcluster_name}-framework",
      }

      labels = {
        "kairosnodetype" = "framework"
      }
    }
  }

  tags = {
    Name = local.perfevalcluster_name
  }
}

resource "aws_autoscaling_group_tag" "cluster_asg_names" {
  for_each               = module.perfevalk8s.eks_managed_node_groups
  autoscaling_group_name = each.value.node_group_autoscaling_group_names[0]

  tag {
    key   = "Name"
    value = "${local.perfevalcluster_name}-${each.key}"

    propagate_at_launch = true
  }

  provisioner "local-exec" {
    command = "aws autoscaling start-instance-refresh --auto-scaling-group-name ${each.value.node_group_autoscaling_group_names[0]} --region ${local.env.aws_region}"
  }
}

resource "aws_security_group_rule" "perfevalk8s_to_default" { #configk8sCluster
  security_group_id        = data.aws_eks_cluster.perfevalk8s.vpc_config[0].cluster_security_group_id
  type                     = "ingress"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  source_security_group_id = local.env.security_group_id
  description              = "perfevalk8s_to_default"
}

resource "aws_security_group_rule" "default_to_perfevalk8s" { #configk8sCluster
  security_group_id        = local.env.security_group_id
  type                     = "ingress"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  source_security_group_id = data.aws_eks_cluster.perfevalk8s.vpc_config[0].cluster_security_group_id
  description              = "default_to_perfevalk8s"
}

resource "null_resource" "perfevalk8s_configk8sCluster" {
  connection {
    host        = aws_instance.k8s_beachhead.public_ip
    type        = "ssh"
    user        = var.beachhead_ami_user
    private_key = file(local.admin_key_pair_file)
  }

  depends_on = [
    module.perfevalk8s,
    aws_instance.k8s_beachhead
  ]
  triggers = {
    beachhead_arn = aws_instance.k8s_beachhead.arn
  }
  provisioner "remote-exec" {
    inline = [
      "kubectl config unset contexts.arn:${data.aws_eks_cluster.perfevalk8s.arn}",
      "aws eks update-kubeconfig --name ${local.perfevalcluster_name} --region ${local.env.aws_region}",
    ]
  }

  provisioner "remote-exec" {
    inline = [
      "kubectl create -f https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/v0.10.0/nvidia-device-plugin.yml",
      "kubectl apply -f infra-templates/gpu-verify.yaml"
    ]
  }
}

locals {
  docker_username = trimspace(base64decode(local.env.docker.username))
  docker_password = trimspace(base64decode(local.env.docker.password))
  docker_auth     = base64encode("${local.docker_username}=${local.docker_password}")
}

resource "kubernetes_secret" "perfdockerreposecret" {
  depends_on = [
    module.perfevalk8s,
    null_resource.perfevalk8s_configk8sCluster
  ]

  metadata {
    name = "perfdockerreposecret"
  }

  type = "kubernetes.io/dockerconfigjson"

  data = {
    ".dockerconfigjson" = jsonencode({
      auths = {
        "https://index.docker.io/v1/" = {
          "username" = local.docker_username
          "password" = local.docker_password
          "auth"     = local.docker_auth
        }
      }
    })
  }
}
