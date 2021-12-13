package ai.aliz.talendtestrunner.service;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;

import ai.aliz.jarvis.context.TestContext;

import static ai.aliz.talendtestrunner.helper.Helper.PROJECT;

public class BigQueryService {

    public BigQuery createBigQueryClient(TestContext context) {
        return BigQueryOptions.newBuilder().setProjectId(context.getParameters().get(PROJECT)).build().getService();
    }
}
