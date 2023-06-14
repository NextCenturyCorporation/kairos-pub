locals {
  env          = data.terraform_remote_state.env.outputs
  perfeval     = data.terraform_remote_state.perfeval.outputs
  project_root = replace(split("moirai-infrastructure/", path.cwd)[1], "/[^/]+/", "..")
  experimentstatus_hostname = "status.${local.perfeval.eks_cluster_name}"

  machine_key_pair_name = local.env.machine_key_pair_name
  machine_key_pair_file = "${local.project_root}/key-pairs/${local.machine_key_pair_name}.pem"
}
