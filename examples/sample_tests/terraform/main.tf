provider "google" {
  version = "3.24.0"
  project = var.project
}

module "BigQuery" {
  source = "bq"
  project = var.project
}
