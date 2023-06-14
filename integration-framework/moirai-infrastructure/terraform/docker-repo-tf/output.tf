output "vpc_id" {
  value = data.terraform_remote_state.environment.outputs.vpc_id
}

output "security_group" {
  value = data.terraform_remote_state.environment.outputs.sg_developer_access_id
}