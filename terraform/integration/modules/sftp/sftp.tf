terraform {
  backend "gcs" {}
}

data "template_file" "pub_key" {
    template = "${file("~/.ssh/id_rsa.pub")}"
}

resource "google_compute_instance" "sftp" {
  name         = "jarvis-test-sftp"
  machine_type = "f1-micro"
  zone         = var.sftp_zone
  boot_disk {
    initialize_params {
      image = "centos-7-v20201216"
    }
  }
  network_interface {
    network = "default"
    access_config {}
  }
}

resource "google_compute_project_metadata" "metadata" {
  metadata = {
    ssh-keys = "${data.template_file.pub_key.template}"
  }
}
