resource "aws_db_subnet_group" "default" {
  name = "moirai rds subnet group"
  subnet_ids = var.development ? [
    local.vpc.subnets.public[1],
    local.vpc.subnets.public[2]
  ] :  [
    local.vpc.subnets.private[1],
    local.vpc.subnets.private[2]
  ]

  tags = {
    Name = "rds_subnet_group"
  }
}
