resource "google_bigquery_dataset" "test" {
  dataset_id = "tf_test"
  project = var.project
  location = var.location
}

resource "google_bigquery_table" "test1" {
  dataset_id = google_bigquery_dataset.test.dataset_id
  table_id = "tf_test1"
  project = var.project
  schema = file("${local.schema_def_folder}/test1.json")
}

resource "google_bigquery_table" "test2" {
  dataset_id = google_bigquery_dataset.test.dataset_id
  table_id = "tf_test2"
  project = var.project
  schema = file("${local.schema_def_folder}/test2.json")
}

resource "google_bigquery_table" "test3" {
  dataset_id = google_bigquery_dataset.test.dataset_id
  table_id = "tf_test3"
  project = var.project
  schema = file("${local.schema_def_folder}/test3.json")
}

data "template_file" "test_view_tpl" {
  template = file("${path.module}/view/test_view.sql")
  vars     = {
    project= var.project
  }
}

resource "google_bigquery_table" "test_view" {
  project     = var.project
  dataset_id  = google_bigquery_dataset.test.dataset_id
  table_id    = "test_view"

  view {
    query = data.template_file.test_view_tpl.rendered
    use_legacy_sql = false
  }

  depends_on = [google_bigquery_table.test1, google_bigquery_table.test2]
}
