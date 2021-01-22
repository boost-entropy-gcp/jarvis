terraform {
  backend "gcs" {}
}

resource "google_compute_instance" "sftp" {
  name         = "jarvis-test-sftp"
  machine_type = "f1-micro"
  zone         = var.sftp_zone
  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-9"
    }
  }
  metadata_startup_script = "${file("${path.module}/jarvis-sftp/startup.sh")}"
  metadata = {
    enable-oslogin = "TRUE"
  }
  network_interface {
    network = "default"
    access_config {}
  }
}

resource "google_project_iam_binding" "service-account-access" {
  project = var.project
  role    = "roles/compute.osAdminLogin"

  members = [
    "serviceAccount:jarvis-tests@nora-ambroz-sandbox.iam.gserviceaccount.com"
  ]
}
