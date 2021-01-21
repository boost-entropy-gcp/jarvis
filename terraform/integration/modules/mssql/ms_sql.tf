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

data "http" "executor_ip" {
  url = "http://ipv4.icanhazip.com"
}

resource "google_sql_database_instance" "mssql_cloudsql" {
  provider         = google-beta
  project          = var.project
  name             = "${var.project}-test-mssql-${random_id.db_name_suffix.hex}"
  database_version = "SQLSERVER_2017_EXPRESS"
  region           = var.sql_region

  settings {
    tier = "db-f1-micro"
    backup_configuration {
      enabled = true
    }
    ip_configuration {
      ipv4_enabled    = true

      authorized_networks {
        name  = "terraform-init"
        value = "${chomp(data.http.executor_ip.body)}/32" //if you just update the database make sure that your IP is whitelisted on the Cloud Console: https://cloud.google.com/sql/docs/mysql/authorize-networks
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

resource "google_sql_user" "db_user" {
  project  = var.project
  name     = "admin"
  instance = google_sql_database_instance.mssql_cloudsql.name
  password = random_string.db_password.result
}

resource "null_resource" "jarvis_ms_db" {
  depends_on = [
    google_sql_database_instance.mssql_cloudsql
  ]
  triggers = {
    file_hash = "${filemd5("${path.module}/jarvis-mssql/db.sql")}"
  }
  provisioner "local-exec" {
    command = "sqlcmd -S ${google_sql_database_instance.mssql_cloudsql.public_ip_address},1433 -d master -U admin -P ${random_string.db_password.result} -i ${path.module}/jarvis-mssql/db.sql"
  }
}

resource "null_resource" "jarvis_ms_table" {
  depends_on = [
    null_resource.jarvis_ms_db
  ]
  triggers = {
    file_hash = "${filemd5("${path.module}/jarvis-mssql/table.sql")}"
    db        = null_resource.jarvis_ms_db.id
  }
  provisioner "local-exec" {
    command = "sqlcmd -S ${google_sql_database_instance.mssql_cloudsql.public_ip_address},1433 -d master -U admin -P ${random_string.db_password.result} -i ${path.module}/jarvis-mssql/table.sql"
  }
}

output "database" {
  value = {
    ip : google_sql_database_instance.mssql_cloudsql.public_ip_address,
    username : google_sql_user.db_user.name,
    password : random_string.db_password.result
  }
}
