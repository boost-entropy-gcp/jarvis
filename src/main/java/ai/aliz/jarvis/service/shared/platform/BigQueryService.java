package ai.aliz.jarvis.service.shared.platform;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;

import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.Context;

import static ai.aliz.jarvis.util.JarvisConstants.PROJECT;

@Service
public class BigQueryService {
    
    public BigQuery createBigQueryClient(Context context) {
        return BigQueryOptions.newBuilder().setProjectId(context.getParameters().get(PROJECT)).build().getService();
    }
}
