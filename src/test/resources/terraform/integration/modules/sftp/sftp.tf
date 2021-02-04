terraform {
  backend "gcs" {}
}

resource "google_compute_instance" "sftp-server" {
  name = "sftp-server"
  machine_type = "f1-micro"
  zone = local.sftp_zone

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-9"
    }
  }

  network_interface {
    network = "default"
    access_config {
      // Ephemeral IP
    }
  }

  metadata = {
    sshKeys = "${local.sftp_user}:${file(local.sftp_public_key_path)}"
  }
  //TODO add user with password
  metadata_startup_script = file(local.init_script)

}

resource "local_file" "sftp-context" {
  filename = "${path.module}/../../../../../../../src/test/resources/integration/sftp-context.json"
  content = <<EOT
  [
    {
  		"id": "SFTP",
  		"contextType": "SFTP",
  		"parameters": {
  			"host": "${google_compute_instance.sftp-server.network_interface[0].access_config[0].nat_ip}",
  			"port": "22",
  			"user": "${local.sftp_user}",
  			"password": "sftp-password",
  			"remoteBasePath": "/home/sftp-user"
  		}
  	}
  ]
  EOT
}

output "sftp-host" {
  value = google_compute_instance.sftp-server.network_interface[0].access_config[0].nat_ip
}
