package ai.aliz.jarvis.service.init.initiator;

import lombok.extern.slf4j.Slf4j;

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
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.Context;
import ai.aliz.jarvis.context.ContextLoader;
import ai.aliz.jarvis.service.init.InitActionService;
import ai.aliz.jarvis.testconfig.InitActionConfig;
import ai.aliz.jarvis.util.TestRunnerUtil;

import static ai.aliz.jarvis.util.JarvisConstants.JSON_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.NO_METADAT_ADDITION;
import static ai.aliz.jarvis.util.JarvisConstants.PROJECT;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.TABLE;
import static ai.aliz.jarvis.util.JarvisConstants.TEST_INIT;

@Slf4j
@Service
public class BQLoadInitiator implements Initiator {
    
    @Autowired
    private ContextLoader contextLoader;
    
    @Autowired
    private InitActionService initActionService;
    
    @Override
    public void doInitAction(InitActionConfig config) {
        doInitActionInner(config, contextLoader.getContext(config.getSystem()));
    }
    
    private void doInitActionInner(InitActionConfig initActionConfig, Context context) {
        Map<String, Object> properties = initActionConfig.getProperties();
        String sourceFormat = (String) properties.get(SOURCE_FORMAT);
        Preconditions.checkArgument(sourceFormat.equalsIgnoreCase(JSON_FORMAT), "Unsupported format: " + sourceFormat);
        
        String table = (String) properties.get(TABLE);
        String sourceContent = TestRunnerUtil.getSourceContentFromConfigProperties(initActionConfig);
        
        JsonArray jsonArray = getJsonArrayFromSource(sourceContent);
        Boolean noMetadatAddition = (Boolean) properties.get(NO_METADAT_ADDITION);
        if (noMetadatAddition == null || !noMetadatAddition) {
            jsonArray = addTableMetadataToJsonArray(jsonArray, table);
        }
        sourceContent = convertToNDJson(jsonArray);
        
        String project = context.getParameter(PROJECT);
        String dataset = TestRunnerUtil.getDatasetNameFromConfigProperties(properties, context);
        TableId tableId = TableId.of(project, dataset, table);
        WriteChannelConfiguration writeChannelConfiguration = WriteChannelConfiguration.newBuilder(tableId)
                                                                                       .setWriteDisposition(JobInfo.WriteDisposition.WRITE_TRUNCATE)
                                                                                       .setFormatOptions(FormatOptions.json()).build();
        
        BigQuery bigQuery = BigQueryOptions.newBuilder().setProjectId(project).build().getService();
        TableDataWriteChannel writer = bigQuery.writer(writeChannelConfiguration);
        try (OutputStream stream = Channels.newOutputStream(writer)) {
            InputStream byteArrayInputStream = new ByteArrayInputStream(sourceContent.getBytes());
            IOUtils.copy(byteArrayInputStream, stream);
        } catch (IOException e) {
            log.error(e.getMessage());
            return;
        }
    
        Job job = writer.getJob();
        try {
            job = job.waitFor();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            return;
        }
        if (job.getStatus().getError() != null || job.getStatus().getExecutionErrors() != null && !job.getStatus().getExecutionErrors().isEmpty()) {
            throw new IllegalStateException(String.format("Failed to execute load job for %s. Error: %s)", initActionConfig, job.getStatus().toString()));
        }
    }
    
    private JsonArray getJsonArrayFromSource(String sourceContent) {
        Gson gson = new Gson();
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
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(jsonArray.iterator(), Spliterator.ORDERED), false)
                            .map(gson::toJson).collect(Collectors.joining("\n"));
    }
    
    private JsonArray addTableMetadataToJsonArray(JsonArray jsonArray, String tableName) {
        jsonArray.iterator().forEachRemaining(e -> e.getAsJsonObject().addProperty(tableName + "_INSERTED_BY", TEST_INIT));
        return jsonArray;
    }
}
