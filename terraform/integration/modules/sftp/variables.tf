variable "project" {
}

variable "region" {
}

variable "sftp_user" {
  type    = string
  default = "jarvis-tests@nora-ambroz-sandbox.iam.gserviceaccount.com"
}

provider "google" {
  project = var.project
  zone    = var.region
}

locals  {
  sftp_zone = "${var.region}-b"
  sftp_public_key_path = "${path.module}/jarvis-sftp/id_rsa.pub"
  init_script_path     = "${path.module}/jarvis-sftp/init_script.sh"
}
