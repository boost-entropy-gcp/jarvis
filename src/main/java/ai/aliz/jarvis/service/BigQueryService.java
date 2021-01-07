package ai.aliz.jarvis.service;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;

import ai.aliz.jarvis.context.Context;

import static ai.aliz.jarvis.helper.Helper.PROJECT;

public class BigQueryService {
    
    public BigQuery createBigQueryClient(Context context) {
        return BigQueryOptions.newBuilder().setProjectId(context.getParameters().get(PROJECT)).build().getService();
    }
}
