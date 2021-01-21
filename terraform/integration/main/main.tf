terraform {
  backend "gcs" {
    bucket = "nora-ambroz-sandbox_tf"
    prefix = "jarvis-test"
  }
}

module "bq" {
  source  = "../modules/bq"
  project = "nora-ambroz-sandbox"
  region  = "europe-west1"
}

module "mssql" {
  source  = "../modules/mssql"
  project = "nora-ambroz-sandbox"
  region  = "europe-west1"
}

module "mysql" {
  source  = "../modules/mysql"
  project = "nora-ambroz-sandbox"
  region  = "europe-west1"
}

module "postgresql" {
  source  = "../modules/postgresql"
  project = "nora-ambroz-sandbox"
  region  = "europe-west1"
}

module "sftp" {
  source  = "../modules/sftp"
  project = "nora-ambroz-sandbox"
  region  = "europe-west1"
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
