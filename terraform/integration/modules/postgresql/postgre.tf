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

resource "google_sql_database_instance" "postgresql_cloudsql" {
  provider            = google-beta
  project             = var.project
  name                = "${var.project}-test-postgresql-${random_id.db_name_suffix.hex}"
  database_version    = "POSTGRES_11"
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

resource "google_sql_user" "psql_db_user" {
  project         = var.project
  name            = var.user_name
  instance        = google_sql_database_instance.postgresql_cloudsql.name
  password        = random_string.db_password.result
  deletion_policy = "ABANDON"
}

resource "google_sql_database" "postgre_sql_database" {
  name     = "JarvisPostgreSQLDB"
  instance = google_sql_database_instance.postgresql_cloudsql.name
}

resource "null_resource" "jarvis_postgre_sql_table" {
  depends_on = [
      google_sql_database_instance.postgresql_cloudsql,
      google_sql_database.postgre_sql_database,
      google_sql_user.psql_db_user
    ]
  provisioner "local-exec" {
    environment = {
      PGPASSWORD = random_string.db_password.result
    }
    command = "psql -d ${google_sql_database.postgre_sql_database.name} -U ${google_sql_user.psql_db_user.name} -h ${google_sql_database_instance.postgresql_cloudsql.public_ip_address} -f ${path.module}/jarvis-postgre/table.sql"
  }
}

resource "local_file" "psql-context" {
  depends_on = [
    google_sql_database_instance.postgresql_cloudsql,
    null_resource.jarvis_postgre_sql_table,
    google_sql_user.psql_db_user
  ]
  filename = "${path.module}/../../../../src/test/resources/integration/psql-context.json"
  content = <<EOT
  [
    {
  		"id": "PostgreSQL",
  		"contextType": "PostgreSQL",
  		"parameters": {
  			"host": "${google_sql_database_instance.postgresql_cloudsql.public_ip_address}",
  			"port": "5432",
  			"database": "${google_sql_database.postgre_sql_database.name}",
  			"user": "${google_sql_user.psql_db_user.name}",
  			"password": "${random_string.db_password.result}"
  		}
  	}
  ]
  EOT
}
