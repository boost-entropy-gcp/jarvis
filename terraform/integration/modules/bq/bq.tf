terraform {
  backend "gcs" {}
}

resource "google_bigquery_dataset" "jarvis_test" {
  dataset_id                  = "jarvis_test"
  location                    = var.bq_region
}

output "dataset" {
  value = {
    dataset : google_bigquery_dataset.jarvis_test.dataset_id
  }
}
