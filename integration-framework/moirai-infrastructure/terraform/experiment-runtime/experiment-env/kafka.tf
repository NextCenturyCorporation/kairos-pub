resource "aws_msk_cluster" "kafka" {
  cluster_name           = local.kafka_config.cluster_name
  kafka_version          = local.kafka_config.version
  number_of_broker_nodes = local.kafka_config.node_count

  broker_node_group_info {
    instance_type   = local.kafka_config.instance_type
    ebs_volume_size = local.kafka_config.volume_size
    client_subnets = [
      local.vpc.subnets.public[0],
      local.vpc.subnets.public[1],
      local.vpc.subnets.public[2]
    ]
    security_groups = [aws_security_group.experiment_runtime.id, local.env.sg_developer_access_id]
    az_distribution = local.kafka_config.brokerazdistribution
  }

  enhanced_monitoring = local.kafka_config.broker_monitoring

  encryption_info {
    encryption_in_transit {
      client_broker = local.kafka_config.encryption_spec
      in_cluster    = true
    }
  }

  tags = {
    Name = local.kafka_config.cluster_name
  }
}

data "aws_msk_broker_nodes" "kafka" {
  cluster_arn = aws_msk_cluster.kafka.arn
}

locals {
  kafka_brokers          = flatten([for k, v in data.aws_msk_broker_nodes.kafka.node_info_list : v.endpoints])
  kafka_broker_endpoints = [for k, v in local.kafka_brokers : "${v}:9092"]
}

output "kafka" {
  value = {
    arn       = aws_msk_cluster.kafka.arn
    brokers   = local.kafka_brokers
    endpoints = local.kafka_broker_endpoints
  }
}
