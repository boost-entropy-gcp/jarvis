package ai.aliz.jarvis.service.initaction.initiator;

import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.RetryOption;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.WriteChannelConfiguration;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.threeten.bp.Duration;

import ai.aliz.jarvis.context.JarvisContextLoader;
import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.config.InitActionConfig;
import ai.aliz.jarvis.util.JarvisUtil;

import static ai.aliz.jarvis.util.JarvisConstants.JSON_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.NO_METADAT_ADDITION;
import static ai.aliz.jarvis.util.JarvisConstants.PROJECT;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.TABLE;
import static ai.aliz.jarvis.util.JarvisConstants.JARVIS_INIT;

@Slf4j
@Service
public class BQLoadInitiator implements Initiator {
    
    @Autowired
    private JarvisContextLoader contextLoader;
    
    private static final Clock clock = Clock.systemUTC();
    
    @Override
    public void doInitAction(InitActionConfig config) {
        doInitActionInner(config, contextLoader.getContext(config.getSystem()));
    }
    
    private void doInitActionInner(InitActionConfig initActionConfig, JarvisContext context) {
        Map<String, Object> properties = initActionConfig.getProperties();
        String sourceFormat = (String) properties.get(SOURCE_FORMAT);
        Preconditions.checkArgument(sourceFormat.equalsIgnoreCase(JSON_FORMAT), "Unsupported format: " + sourceFormat);
        
        String table = (String) properties.get(TABLE);
        String sourceContent = JarvisUtil.getSourceContentFromConfigProperties(initActionConfig);
        
        JsonArray jsonArray = getJsonArrayFromSource(sourceContent);
        Boolean noMetadatAddition = (Boolean) properties.get(NO_METADAT_ADDITION);
        if (noMetadatAddition == null || !noMetadatAddition) {
            jsonArray = addTableMetadataToJsonArray(jsonArray, table);
        }
        sourceContent = convertToNDJson(jsonArray);
        
        String project = context.getParameter(PROJECT);
        String dataset = JarvisUtil.getDatasetNameFromConfigProperties(properties, context);
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
            throw Lombok.sneakyThrow(e);
        }
        
        Job job = writer.getJob();
        final Stopwatch sw = Stopwatch.createStarted();
        final Job completedJob;
        if (job.isDone()) {
            completedJob = job.reload();
        } else {
            log.info("Waiting for job {}...", job.getJobId());
            try {
                completedJob = job.waitFor(
                        RetryOption.maxRetryDelay(Duration.ofSeconds(1)),
                        RetryOption.initialRetryDelay(Duration.ofSeconds(1)),
                        RetryOption.totalTimeout(Duration.ofMillis(Math.max(clock.instant().plus(30, ChronoUnit.MINUTES).toEpochMilli() - clock.millis(), 1L))));
            } catch (InterruptedException e) {
                log.error("Error during job in " + sw, e);
                throw Lombok.sneakyThrow(e);
            }
        }
        
        if (completedJob.getStatus().getError() == null && completedJob.isDone()) {
            log.info("Query job finished successfully ({}) in {}", completedJob.getJobId().getJob(), sw);
        } else {
            String errors = completedJob.getStatus().getExecutionErrors().stream().map(BigQueryError::toString).collect(Collectors.joining("\n"));
            throw new IllegalStateException(String.format("Failed to execute load job for %s. Error: %s)", initActionConfig, errors));
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
        jsonArray.iterator().forEachRemaining(e -> e.getAsJsonObject().addProperty(tableName + "_INSERTED_BY", JARVIS_INIT));
        return jsonArray;
    }
}
