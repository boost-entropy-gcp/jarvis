variable "project" {
  type = string
}

variable "location" {
  type = string
  default = "europe-west3"
}

locals {
  schema_def_folder          = "${path.module}/schemas"
}
