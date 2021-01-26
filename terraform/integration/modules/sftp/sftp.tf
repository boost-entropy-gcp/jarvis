terraform {
  backend "gcs" {}
}

resource "random_string" "sftp_password" {
  length  = 12
  special = false
}

resource "google_container_cluster" "sftp_server" {
  project            = var.project
  name               = "sftp-server"
  location           = local.sftp_zone
  initial_node_count = 1

  node_config {
    preemptible  = false
    machine_type = "n1-standard-1"

    metadata = {
      disable-legacy-endpoints = "true"
    }

    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
    ]
  }
}

resource "null_resource" "set_kubernetes_credentials" {
  provisioner "local-exec" {
    command = "gcloud container clusters get-credentials ${google_container_cluster.sftp_server.name} --zone ${local.sftp_zone} --project ${var.project}"
  }
}

resource "kubernetes_secret" "sftp_public_key" {
  metadata {
    name = "sftp-public-key"
  }

  data = {
    "sftp.pub" = file(local.sftp_public_key_path)
  }
}

resource "kubernetes_pod" "sftp_server_pod" {
  depends_on = [google_container_cluster.sftp_server]
  metadata {
    name = "sftp-server"
    labels = {
      app = "sftp-server"
    }
  }

  spec {
    container {
      image = "atmoz/sftp"
      name  = "sftp-server"

      port {
        container_port = 22
        host_port      = 2222
      }

      volume_mount {
        mount_path = "/home/sftp-user/.ssh/keys/"
        name       = "sftp-public-key-volume"
      }

      env {
        name  = "SFTP_USERS"
        value = "${var.sftp_user}:${random_string.sftp_password.result}:::out"
      }

      stdin = true
      tty   = true

    }
    volume {
      name = "sftp-public-key-volume"
      secret {
        secret_name = "sftp-public-key"
      }
    }
  }
}

resource "kubernetes_service" "sftp_service" {
  metadata {
    name = "sftp-service"
  }
  spec {
    selector = {
      app = kubernetes_pod.sftp_server_pod.metadata[0].labels.app
    }
    port {
      port        = 2222
      target_port = 22
    }

    type = "LoadBalancer"
  }
}

resource "local_file" "sftp-context" {
  depends_on = [
    kubernetes_service.sftp_service,
    kubernetes_pod.sftp_server_pod
  ]
  filename = "${path.module}/../../../../src/test/resources/integration/sftp-context.json"
  content = <<EOT
  [
    {
  		"id": "SFTP",
  		"contextType": "SFTP",
  		"parameters": {
  			"host": "${kubernetes_service.sftp_service.spec[0].cluster_ip}",
  			"port": "2222",
  			"user": "${var.sftp_user}",
  			"password": "${random_string.sftp_password.result}",
  			"remote_path": "/home/sftp-user/in"
  		}
  	}
  ]
  EOT
}

