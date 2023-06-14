resource "aws_db_subnet_group" "neptune" {
  name = "neptune-serverless-cluster-subnetgroup"
  subnet_ids = [
    local.env.vpc.subnets.private[0],
    local.env.vpc.subnets.private[1],
    local.env.vpc.subnets.private[2],
    local.env.vpc.subnets.public[0],
    local.env.vpc.subnets.public[1],
    local.env.vpc.subnets.public[2]
  ]

  tags = {
    Name = "Neptune-Serverless-Cluster-Group"
  }
}
