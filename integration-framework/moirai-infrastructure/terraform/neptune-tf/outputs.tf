output "write_ep" {
  value = aws_neptune_cluster_instance.node.2.writer ? aws_neptune_cluster_instance.node.2.endpoint : ""
}

output "reader_ep1" {
  value = aws_neptune_cluster_instance.node.1.writer ? "" : aws_neptune_cluster_instance.node.1.endpoint
}

output "reader_ep2" {
  value = aws_neptune_cluster_instance.node.0.writer ? "" : aws_neptune_cluster_instance.node.0.endpoint
}
