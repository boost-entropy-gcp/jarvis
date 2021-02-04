terraform {
  backend "gcs" {}
}

resource "google_bigquery_dataset" "jarvis_test" {
  dataset_id                  = "jarvis_test"
  location                    = var.bq_region
}

resource "local_file" "bq-context" {
  filename = "${path.module}/../../../../../../../../src/test/resources/integration/bq-context.json"
  content = <<EOT
    [
  	  {
  		"id": "BQ",
  		"contextType": "BigQuery",
  		"parameters": {
  			"project": "${var.project}"
  		}
  	  }
    ]
EOT
}
