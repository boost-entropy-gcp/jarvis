package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.Context;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;

import static ai.aliz.talendtestrunner.helper.Helper.PROJECT;

public class BigQueryService {

    public BigQuery createBigQueryClient(Context context) {
        return BigQueryOptions.newBuilder().setProjectId(context.getParameters().get(PROJECT)).build().getService();
    }
}
