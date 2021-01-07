package ai.aliz.jarvis.service.initiator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.WriteChannelConfiguration;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.Context;
import ai.aliz.jarvis.context.ContextLoader;
import ai.aliz.jarvis.service.InitActionService;
import ai.aliz.jarvis.testconfig.InitActionConfig;
import ai.aliz.jarvis.util.TestRunnerUtil;

import static ai.aliz.jarvis.helper.Helper.DATASET;
import static ai.aliz.jarvis.helper.Helper.PROJECT;
import static ai.aliz.jarvis.helper.Helper.SOURCE_FORMAT;
import static ai.aliz.jarvis.helper.Helper.TABLE;
import static ai.aliz.jarvis.helper.Helper.TEST_INIT;

@Service
public class BQLoadInitiator implements Initiator {
    
    @Autowired
    private ContextLoader contextLoader;
    
    @Autowired
    private InitActionService initActionService;
    
    @Override
    public void doInitAction(InitActionConfig config) {
        doBigQueryInitAction(config, contextLoader.getContext(config.getSystem()));
    }
    
    public void doBigQueryInitAction(InitActionConfig initActionConfig, Context context) {
        String project = context.getParameter(PROJECT);
        Map<String, Object> properties = initActionConfig.getProperties();
        String dataset = (String) properties.get(DATASET);
        
        String datasetNamePrefix = context.getParameter("datasetNamePrefix");
        if (datasetNamePrefix != null) {
            dataset = datasetNamePrefix + dataset;
        }
        String table = (String) properties.get(TABLE);
        
        BigQuery bigQuery = BigQueryOptions.newBuilder().setProjectId(project).build().getService();
        
        TableId tableId = TableId.of(project, dataset, table);
        
        String sourceContent = TestRunnerUtil.getSourceContentFromConfigProperties(initActionConfig);
        
        FormatOptions formatOption;
        
        String sourceFormat = (String) properties.get(SOURCE_FORMAT);
        switch (sourceFormat) {
            case "json": {
                Gson gson = new Gson();
                JsonArray jsonArray = getJsonArrayFromSource(sourceContent, gson);
                Boolean noMetadatAddition = (Boolean) properties.get("noMetadatAddition");
                if (noMetadatAddition == null || !noMetadatAddition) {
                    jsonArray = addTableMetadata(jsonArray, table);
                }
                sourceContent = convertToNDJson(jsonArray);
                formatOption = FormatOptions.json();
                break;
            }
            default:
                throw new RuntimeException("Unsupported format: " + sourceFormat);
        }
        
        WriteChannelConfiguration writeChannelConfiguration = WriteChannelConfiguration.newBuilder(tableId)
                                                                                       .setWriteDisposition(JobInfo.WriteDisposition.WRITE_TRUNCATE)
                                                                                       .setFormatOptions(formatOption).build();
        TableDataWriteChannel writer = bigQuery.writer(writeChannelConfiguration);
        try (OutputStream stream = Channels.newOutputStream(writer)) {
            InputStream byteArrayInputStream = new ByteArrayInputStream(sourceContent.getBytes());
            IOUtils.copy(byteArrayInputStream, stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Job job = writer.getJob();
        
        try {
            job = job.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        if (job.getStatus().getError() != null || job.getStatus().getExecutionErrors() != null && !job.getStatus().getExecutionErrors().isEmpty()) {
            throw new RuntimeException(String.format("Failed to execute load job for %s. Error: %s)", initActionConfig, job.getStatus().toString()));
        }
    }
    
    private JsonArray getJsonArrayFromSource(String sourceContent, Gson gson) {
        JsonElement jsonElement = gson.fromJson(sourceContent, JsonElement.class);
        if (jsonElement instanceof JsonArray) {
            return (JsonArray) jsonElement;
        } else {
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jsonElement);
            
            return jsonArray;
        }
    }
    
    private String convertToNDJson(JsonArray jsonArray) {
        Gson gson = new Gson();
        String ndJson = StreamSupport.stream(Spliterators.spliteratorUnknownSize(jsonArray.iterator(), Spliterator.ORDERED), false)
                                     .map(gson::toJson).collect(Collectors.joining("\n"));
        
        return ndJson;
    }
    
    private JsonArray addTableMetadata(JsonArray jsonArray, String tableName) {
        jsonArray.iterator().forEachRemaining(e -> e.getAsJsonObject().addProperty(tableName + "_INSERTED_BY", TEST_INIT));
        return jsonArray;
    }
}
