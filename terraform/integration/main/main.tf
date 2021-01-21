terraform {
  backend "gcs" {
    bucket = "nora-ambroz-sandbox_tf"
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
