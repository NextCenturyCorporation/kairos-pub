data "aws_availability_zones" "available" {}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "3.14.0"

  name                 = "${var.project_name}-vpc"
  cidr                 = "12.1.0.0/16"
  azs                  = data.aws_availability_zones.available.names
  private_subnets      = ["12.1.0.0/23", "12.1.2.0/23", "12.1.4.0/23", "12.1.6.0/23"]
  public_subnets       = ["12.1.8.0/23", "12.1.10.0/23", "12.1.12.0/23", "12.1.14.0/23"]
  enable_nat_gateway   = true
  single_nat_gateway   = true
  enable_dns_hostnames = true

  #   tags = {
  #     "kubernetes.io/cluster/${local.perfevalcluster_name}" = "shared"
  #   }

  #   public_subnet_tags = {
  #     "kubernetes.io/cluster/${local.perfevalcluster_name}" = "shared"
  #     "kubernetes.io/role/elb"                              = "1"
  #   }

  #   private_subnet_tags = {
  #     "kubernetes.io/cluster/${local.perfevalcluster_name}" = "shared"
  #     "kubernetes.io/role/internal-elb"                     = "1"
  #   }
}
