resource "aws_neptune_cluster_parameter_group" "group" {
  family      = "neptune1.2"
  name        = "terraform-neptune"
  description = "neptune cluster parameter group"

  parameter {
    name  = "neptune_enable_audit_log"
    value = 1
  }
  parameter {
    name  = "neptune_enforce_ssl"
    value = 0
  }
  parameter {
    name  = "neptune_lookup_cache"
    value = 1
  }
  parameter {
    name  = "neptune_query_timeout"
    value = 300000
  }
  parameter {
    name  = "neptune_streams"
    value = 1
  }
}
