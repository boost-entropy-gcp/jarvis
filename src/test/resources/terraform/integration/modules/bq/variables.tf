variable "project" {
}

variable "region" {
}

provider "google" {
  project = var.project
  zone    = var.region
}

variable "bq_region" {
  type    = string
  default = "europe-west1"
}
