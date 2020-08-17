package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.Context;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;

public class BigQueryService {

    public BigQuery createBigQueryClient(Context context) {
        return BigQueryOptions.newBuilder().setProjectId(context.getParameters().get("project")).build().getService();
    }
}
