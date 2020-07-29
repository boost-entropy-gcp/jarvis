package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import ai.aliz.talendtestrunner.db.MxSQLQueryExecutor;
import ai.aliz.talendtestrunner.testconfig.InitActionConfig;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class InitActionService {

    public static final String TEST_INIT = "test_init";
    @Autowired
    private MxSQLQueryExecutor mxSQLQueryExecutor;

    @Autowired
    private BigQueryExecutor bigQueryExecutor;

    @Autowired
    private ExecutorServiceImpl executorService;

    @Autowired
    private SftpService sftpService;

    public void run(List<InitActionConfig> initActionConfigs, ContextLoader contextLoader) {
        List<Runnable> initActionRunnables = initActionConfigs.stream()
                .map(initActionConfig -> new Runnable() {
                    @Override
                    public void run() {
                        InitActionService.this.run(initActionConfig, contextLoader);
                    }
                })
                .collect(Collectors.toList());

        executorService.executeRunnablesInParallel(initActionRunnables, 5, TimeUnit.MINUTES);
    }

    public void run(InitActionConfig initActionConfig, ContextLoader contextLoader) {

        try {
            log.info("========================================================");
            log.info("Executing initaction: {}", initActionConfig);
            String type = initActionConfig.getType();
            String system = initActionConfig.getSystem();
            Context context = contextLoader.getContext(system);
            switch (type) {
                case "BQLoad": {
                    String project = context.getParameter("project");
                    Map<String, Object> properties = initActionConfig.getProperties();
                    String dataset = (String) properties.get("dataset");

                    String datasetNamePrefix = context.getParameter("datasetNamePrefix");
                    if (datasetNamePrefix != null) {
                        dataset = datasetNamePrefix + dataset;
                    }
                    String table = (String) properties.get("table");

                    BigQuery bigQuery = BigQueryOptions.newBuilder().setProjectId(project).build().getService();

                    TableId tableId = TableId.of(project, dataset, table);

                    String sourceContent = TestRunnerUtil.getSourceContentFromConfigProperties(initActionConfig);

                    FormatOptions formatOption;

                    String sourceFormat = (String) properties.get("sourceFormat");
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
                    break;

                }
                case "SQLExec": {
                    switch (context.getType()) {
                        case MSSQL:
                        case MySQL:
                            mxSQLQueryExecutor.executeScript(TestRunnerUtil.getSourceContentFromConfigProperties(initActionConfig), context);
                            break;
                        case BigQuery:
                            bigQueryExecutor.executeScript(TestRunnerUtil.getSourceContentFromConfigProperties(initActionConfig), context);
                            break;
                        default:
                            throw new UnsupportedOperationException("Not supported context type: " + context.getType());
                    }

                    break;

                }
                case "SFTPLoad": {
                    sftpService.loadFilesToSftp(initActionConfig, context);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Not supported type: " + type);
            }

            log.info("InitAction {} finished", initActionConfig);
            log.info("========================================================");
        } catch (Exception e) {
            throw new RuntimeException(String.format("InitAction: %s failed", initActionConfig), e);
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
