terraform {
  backend "gcs" {
    bucket = "${project_name}_tf"
    prefix = "jarvis-test"
  }
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
}

module "mysql" {
  source  = "../modules/mysql"
  project = var.project
  region  = var.region
}

module "postgresql" {
  source  = "../modules/postgresql"
  project = var.project
  region  = var.region
}

module "sftp" {
  source  = "../modules/sftp"
  project = var.project
  region  = var.region
}

resource "local_file" "integration-contexts" {
  content     = "right here"
  filename    = "${path.module}/integration-contexts.json"
}

output "bq_dataset" {
  value = module.bq.dataset
}

output "mssql_database" {
  value = module.mssql.database
}

output "mysql_database" {
  value = module.mysql.database
}

output "postgresql_database" {
  value = module.postgresql.database
}
