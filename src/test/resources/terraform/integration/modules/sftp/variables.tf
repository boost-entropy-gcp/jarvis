variable "project" {
}

variable "region" {
}

provider "google" {
  project = var.project
  zone    = var.region
}

locals  {
  sftp_zone = "${var.region}-b"
  sftp_user = "sftp-user"
  sftp_public_key_path = "${path.module}/jarvis-sftp/id_rsa.pub"
  sftp_private_key_path = "${path.module}/jarvis-sftp/id_rsa"
  init_script = "${path.module}/jarvis-sftp/init-script.sh"
}
