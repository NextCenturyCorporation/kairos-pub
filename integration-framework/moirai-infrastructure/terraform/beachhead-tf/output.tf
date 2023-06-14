output "vpc_id" {
  value = local.env.vpc_id
}

output "security_group" {
  value = local.env.sg_developer_access_id
}

output "public_dns" {
  value = join(",", aws_instance.beach_head.*.public_dns)
}

output "public_ip" {
  value = aws_instance.beach_head.public_ip
}

output "Connect" {
  value = formatlist(
    "ssh -i %s ubuntu@%s",
    local.key_pair_file,
    aws_instance.beach_head.*.public_dns,
  )
}
