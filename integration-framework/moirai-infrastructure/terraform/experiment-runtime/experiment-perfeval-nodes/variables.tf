variable "experiment" {
  type    = string
  default = "default"
}

# CPU nodes
variable "cpu_nodetype" { #if nodetype is one of [p2*, p3*, inf1*, g3*, g4*, f1*] set perfevalcluster_nodetype_gpu = true
  type    = string
  default = "m5.xlarge"
}

variable "cpu_nodevolsize" {
  type    = string
  default = "500"
}

variable "cpu_nodesmax" {
  type    = string
  default = "5"
}
variable "cpu_nodesmin" {
  type    = string
  default = "1"
}

# GPU nodes
variable "gpu_nodetype" { #if nodetype is one of [p2*, p3*, inf1*, g3*, g4*, f1*] set perfevalcluster_gpu_nodetype_gpu = true
  type    = string
  default = "p3.2xlarge"
}
variable "gpu_nodevolsize" {
  type    = string
  default = "1000"
}

variable "gpu_nodesmax" {
  type    = string
  default = "5"
}

variable "gpu_nodesmin" {
  type    = string
  default = "1"
}
