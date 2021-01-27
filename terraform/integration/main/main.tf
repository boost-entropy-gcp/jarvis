terraform {
  backend "gcs" {
    bucket = "nora-ambroz-sandbox_tf"
    prefix = "jarvis-test"
  }
}

data "http" "executor_ip" {
  url = "http://ipv4.icanhazip.com"
}

module "bq" {
  source  = "../modules/bq"
  project = var.project
  region  = var.region
}

module "mssql" {
  source  = "../modules/mssql"
  project = var.project
  region  = var.region
  //authorizes the local IP, if you just update the database make sure that your IP is whitelisted on the Cloud Console: https://cloud.google.com/sql/docs/mysql/authorize-networks
  local_ip = "${chomp(data.http.executor_ip.body)}/32"
}

module "mysql" {
  source  = "../modules/mysql"
  project = var.project
  region  = var.region
  //authorizes the local IP, if you just update the database make sure that your IP is whitelisted on the Cloud Console: https://cloud.google.com/sql/docs/mysql/authorize-networks
  local_ip = "${chomp(data.http.executor_ip.body)}/32"
}

module "postgresql" {
  source  = "../modules/postgresql"
  project = var.project
  region  = var.region
  //authorizes the local IP, if you just update the database make sure that your IP is whitelisted on the Cloud Console: https://cloud.google.com/sql/docs/mysql/authorize-networks
  local_ip = "${chomp(data.http.executor_ip.body)}/32"
}

// TODO
/*
module "sftp" {
  source     = "../modules/sftp"
  project    = var.project
  region     = var.region
  sftp_user  = var.user
}
*/

resource "local_file" "db_authorization" {
  filename = "${path.module}/../../../.github/workflows/db_authorization.json"
  content = <<EOT
   {
  		"mssql": "${module.mssql.mssql_instance_id}",
  		"mysql": "${module.mysql.mysql_instance_id}",
  		"psql": "${module.postgresql.psql_instance_id}",
  		"authorized": "${chomp(data.http.executor_ip.body)}/32"
   }
  EOT
}
