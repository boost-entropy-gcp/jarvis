package ai.aliz.talendtestrunner.service;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;

import ai.aliz.jarvis.context.JarvisContext;

import static ai.aliz.talendtestrunner.helper.Helper.PROJECT;

public class BigQueryService {

    public BigQuery createBigQueryClient(JarvisContext context) {
        return BigQueryOptions.newBuilder().setProjectId(context.getParameters().get(PROJECT)).build().getService();
    }
}
