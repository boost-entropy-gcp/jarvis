variable "project" {
}

variable "region" {
}

provider "google" {
  project = var.project
  zone    = var.region
}

variable "sftp_zone" {
  type    = string
  default = "europe-west1-b"
}
