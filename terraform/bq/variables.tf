variable "project" {
  type = string
  default = "bertalan-bodroghelyi-sandbox"
}

variable "location" {
  type = string
  default = "europe-west3"
}

locals {
  schema_def_folder          = "${path.module}/schemas"
}
