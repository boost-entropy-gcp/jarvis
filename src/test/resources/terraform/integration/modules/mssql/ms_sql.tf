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

resource "google_sql_database_instance" "mssql_cloudsql" {
  provider            = google-beta
  project             = var.project
  name                = "${var.project}-test-mssql-${random_id.db_name_suffix.hex}"
  database_version    = "SQLSERVER_2017_EXPRESS"
  region              = var.sql_region
  deletion_protection = false

  settings {
    tier = "db-custom-1-3840"
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

resource "google_sql_user" "mssql_db_user" {
  project         = var.project
  name            = var.user_name
  instance        = google_sql_database_instance.mssql_cloudsql.name
  password        = random_string.db_password.result
  deletion_policy = "ABANDON"
}

resource "null_resource" "jarvis_ms_db" {
  depends_on = [
    google_sql_database_instance.mssql_cloudsql,
    google_sql_user.mssql_db_user
  ]
  triggers = {
    file_hash = filemd5("${path.module}/jarvis-mssql/db.sql")
  }
  provisioner "local-exec" {
    command = "sqlcmd -S ${google_sql_database_instance.mssql_cloudsql.public_ip_address},1433 -d master -U ${google_sql_user.mssql_db_user.name} -P ${random_string.db_password.result} -i ${path.module}/jarvis-mssql/db.sql"
  }
}

resource "null_resource" "jarvis_ms_table" {
  depends_on = [
    null_resource.jarvis_ms_db,
    google_sql_user.mssql_db_user
  ]
  triggers = {
    file_hash = filemd5("${path.module}/jarvis-mssql/table.sql")
    db        = null_resource.jarvis_ms_db.id
  }
  provisioner "local-exec" {
    command = "sqlcmd -S ${google_sql_database_instance.mssql_cloudsql.public_ip_address},1433 -d master -U ${google_sql_user.mssql_db_user.name} -P ${random_string.db_password.result} -i ${path.module}/jarvis-mssql/table.sql"
  }
}

resource "local_file" "mssql-context" {
  depends_on = [
    google_sql_database_instance.mssql_cloudsql,
    null_resource.jarvis_ms_db,
    google_sql_user.mssql_db_user
  ]
  filename = "${path.module}/../../../../../../../src/test/resources/integration/mssql-context.json"
  content = <<EOT
  [
    {
  		"id": "MSSQL",
  		"contextType": "MSSQL",
  		"parameters": {
  			"host": "${google_sql_database_instance.mssql_cloudsql.public_ip_address}",
  			"port": "1433",
  			"database": "JarvisMSDB",
  			"user": "${google_sql_user.mssql_db_user.name}",
  			"password": "${random_string.db_password.result}"
  		}
  	}
  ]
  EOT
}

output "mssql_instance_id" {
  value       = google_sql_database_instance.mssql_cloudsql.name
}
