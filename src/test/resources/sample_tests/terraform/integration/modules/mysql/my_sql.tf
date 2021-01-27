terraform {
  backend "gcs" {}
}

resource "random_string" "db_password" {
  length      = 12
  special     = false
  min_upper   = 1
  min_lower   = 1
  min_numeric = 1
}

resource "random_id" "db_name_suffix" {
  byte_length = 4
}

resource "google_sql_database_instance" "mysql_cloudsql" {
  provider            = google-beta
  project             = var.project
  name                = "${var.project}-test-mysql-${random_id.db_name_suffix.hex}"
  database_version    = "MYSQL_8_0"
  region              = var.sql_region
  deletion_protection = false

  settings {
    tier = "db-f1-micro"
    backup_configuration {
      enabled = true
    }
    ip_configuration {
      ipv4_enabled    = true

      authorized_networks {
        name  = "terraform-init"
        value = var.local_ip
      }
    }
  }
  lifecycle {
    ignore_changes = [
      settings //to ignore the changes on the IP whitelistings
    ]
  }
  root_password = random_string.db_password.result
}

resource "google_sql_user" "mysql_db_user" {
  project         = var.project
  name            = var.user_name
  instance        = google_sql_database_instance.mysql_cloudsql.name
  password        = random_string.db_password.result
  deletion_policy = "ABANDON"
}

resource "google_sql_database" "my_sql_database" {
  name     = "JarvisMySQLDB"
  instance = google_sql_database_instance.mysql_cloudsql.name
}

resource "null_resource" "jarvis_my_sql_table" {
  depends_on = [
    google_sql_database.my_sql_database,
    google_sql_user.mysql_db_user
  ]
  triggers = {
    file_hash = filemd5("${path.module}/jarvis-mysql/table.sql")
    db        = google_sql_database.my_sql_database.id
  }
  provisioner "local-exec" {
    command = "mysql -u ${google_sql_user.mysql_db_user.name} -p${random_string.db_password.result} -h ${google_sql_database_instance.mysql_cloudsql.public_ip_address} < ${path.module}/jarvis-mysql/table.sql --binary-mode"
  }
}

resource "local_file" "mysql-context" {
  depends_on = [
    google_sql_database_instance.mysql_cloudsql,
    null_resource.jarvis_my_sql_table,
    google_sql_user.mysql_db_user
  ]
  filename = "${path.module}/../../../../src/test/resources/integration/mysql-context.json"
  content = <<EOT
  [
    {
  		"id": "MySQL",
  		"contextType": "MySQL",
  		"parameters": {
  			"host": "${google_sql_database_instance.mysql_cloudsql.public_ip_address}",
  			"port": "3306",
  			"database": "${google_sql_database.my_sql_database.name}",
  			"user": "${google_sql_user.mysql_db_user.name}",
  			"password": "${random_string.db_password.result}"
  		}
  	}
  ]
  EOT
}

output "mysql_instance_id" {
  value       = google_sql_database_instance.mysql_cloudsql.name
}
