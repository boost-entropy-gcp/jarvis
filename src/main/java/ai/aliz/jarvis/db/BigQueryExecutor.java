package ai.aliz.jarvis.db;

import lombok.AllArgsConstructor;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.google.api.client.util.Lists;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aliz.jarvis.service.shared.platform.BigQueryService;
import ai.aliz.jarvis.service.shared.ExecutorServiceWrapper;
import ai.aliz.jarvis.context.Context;
import ai.aliz.jarvis.util.TestRunnerUtil;

import static ai.aliz.talendtestrunner.helper.Helper.TEST_INIT;

@Component
@AllArgsConstructor
@Slf4j
public class BigQueryExecutor implements QueryExecutor {
    
    private ExecutorServiceWrapper executorService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private BigQueryService bigQueryService;
    
    @Override
    public void executeStatement(String query, Context context) {
        TableResult queryResult = executeQueryAndGetResult(query, context);
        log.debug("Query result {}", queryResult.iterateAll());
    }
    
    @Override
    public String executeQuery(String query, Context context) {
        TableResult queryResult = executeQueryAndGetResult(query, context);
        ArrayNode result = bigQueryResultToJsonArrayNode(queryResult);
        return result.toString();
    }
    
    @Override
    public void executeScript(String query, Context context) {
        List<String> deletes = Lists.newArrayList();
        List<String> inserts = Lists.newArrayList();
        Arrays.stream(query.split(";"))
              .map(String::trim)
              .filter(e -> !e.isEmpty())
              .map(String::toUpperCase)
              .forEach(e -> {
                  if (e.contains("DELETE FROM")) {
                      deletes.add(e);
                  } else {
                      inserts.add(e);
                  }
              });
        
        List<Runnable> deleteRunnables = statementsToRunnables(context, deletes);
        List<Runnable> insertRunnables = statementsToRunnables(context, inserts);
        
        executorService.executeRunnablesInParallel(deleteRunnables, 60, TimeUnit.SECONDS);
        executorService.executeRunnablesInParallel(insertRunnables, 60, TimeUnit.SECONDS);
    }
    
    public int insertedRowCount(String tableId, String tableName, Context context) {
        TableResult tableResult = executeQueryAndGetResult("SELECT COUNT(*) FROM `" + tableId + "`WHERE " + tableName + "_INSERTED_BY != '" + TEST_INIT + "'", context);
        long count = tableResult.getValues().iterator().next().get(0).getLongValue();
        return (int) count;
    }
    
    public Long getTableLastModifiedAt(Context context, String project, String dataset, String table) {
        BigQuery bigQuery = getBigQueryClient(context);
        log.info("Getting last modified at for table: {}.{}.{}", project, dataset, table);
        Table bqTable = bigQuery.getTable(TableId.of(project, dataset, table));
        
        return bqTable.getLastModifiedTime();
    }
    
    private BigQuery getBigQueryClient(Context context) {
        return bigQueryService.createBigQueryClient(context);
    }
    
    private List<Runnable> statementsToRunnables(Context context, List<String> statements) {
        return statements.stream()
                         .map(statement -> (Runnable) () -> executeStatement(statement, context))
                         .collect(Collectors.toList());
    }
    
    private TableResult executeQueryAndGetResult(String query, Context context) {
        String completedQuery = TestRunnerUtil.resolvePlaceholders(query, context.getParameters());
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(completedQuery).build();
        
        BigQuery bigQuery = getBigQueryClient(context);
        try {
            log.info("Executing {}", query);
            return bigQuery.query(queryConfig);
        } catch (Exception e) {
            log.error("Failed to execute: " + completedQuery, e);
            throw Lombok.sneakyThrow(e);
        }
    }
    
    private ArrayNode bigQueryResultToJsonArrayNode(TableResult queryResult) {
        FieldList schema = queryResult.getSchema().getFields();
        Iterator<FieldValueList> fieldValueListIterator = queryResult.iterateAll().iterator();
        ArrayNode result = objectMapper.createArrayNode();
        
        while (fieldValueListIterator.hasNext()) {
            FieldValueList row = fieldValueListIterator.next();
            ObjectNode recordNode = bigqueryRowToJsonNode(schema, row);
            result.add(recordNode);
        }
        return result;
    }
    
    private ObjectNode bigqueryRowToJsonNode(FieldList schema, FieldValueList row) {
        ObjectNode recordNode = objectMapper.createObjectNode();
        Iterator<Field> schemaIterator = schema.iterator();
        for (FieldValue value : row) {
            Field fieldSchema = schemaIterator.next();
            createAndAddJsonNodeToParent(fieldSchema, value, recordNode);
        }
        return recordNode;
    }
    
    private void createAndAddJsonNodeToParent(Field fieldSchema, FieldValue fieldValue, ObjectNode parent) {
        BaseJsonNode baseJsonNode;
        LegacySQLTypeName fieldType = fieldSchema.getType();
        try {
            
            if (fieldValue.isNull()) {
                baseJsonNode = NullNode.getInstance();
            } else if (fieldType.equals(LegacySQLTypeName.BOOLEAN)) {
                baseJsonNode = BooleanNode.valueOf(fieldValue.getBooleanValue());
            } else if (fieldType.equals(LegacySQLTypeName.TIMESTAMP)) {
                baseJsonNode = timestampFieldToJsonNode(fieldValue);
            } else if (fieldType.equals(LegacySQLTypeName.RECORD)) {
                if (fieldSchema.getMode() == Field.Mode.REPEATED) {
                    ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
                    baseJsonNode = arrayNode;
                    
                    for (FieldValue arrayElementFieldValue : fieldValue.getRepeatedValue()) {
                        FieldList subFields = fieldSchema.getSubFields();
                        BaseJsonNode arrayElementNode;
                        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
                        FieldValueList recordValue = arrayElementFieldValue.getRecordValue();
                        int i = 0;
                        for (Field arrayElementField : subFields) {
                            createAndAddJsonNodeToParent(arrayElementField, recordValue.get(i), objectNode);
                            i++;
                        }
                        arrayElementNode = objectNode;
                        
                        arrayNode.add(arrayElementNode);
                    }
                } else {
                    ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
                    baseJsonNode = objectNode;
                    FieldValueList recordValue = fieldValue.getRecordValue();
                    FieldList subFields = fieldSchema.getSubFields();
                    int i = 0;
                    for (Field arrayElementField : subFields) {
                        createAndAddJsonNodeToParent(arrayElementField, recordValue.get(i), objectNode);
                        i++;
                    }
                }
            } else if (fieldSchema.getMode() == Field.Mode.REPEATED) {
                
                ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
                baseJsonNode = arrayNode;
                
                for (FieldValue arrayElementFieldValue : fieldValue.getRepeatedValue()) {
                    LegacySQLTypeName type = fieldSchema.getType();
                    BaseJsonNode arrayElementNode;
                    
                    if (type.equals(LegacySQLTypeName.STRING)) {
                        String stringValue = arrayElementFieldValue.getStringValue();
                        arrayElementNode = TextNode.valueOf(stringValue);
                    } else {
                        throw new UnsupportedOperationException("Not supported primitive type in array. " + type);
                    }
                    arrayNode.add(arrayElementNode);
                }
            } else {
                String stringValue = fieldValue.getStringValue();
                baseJsonNode = TextNode.valueOf(stringValue);
            }
        } catch (Exception e) {
            log.error(String.format("%s %s Failed to parse field \n%s \nwith schema \n%s", fieldType, fieldType.equals(LegacySQLTypeName.RECORD), fieldValue, fieldSchema));
            throw Lombok.sneakyThrow(e);
        }
        parent.set(fieldSchema.getName(), baseJsonNode);
    }
    
    private ValueNode timestampFieldToJsonNode(FieldValue fieldValue) {
        if (fieldValue.isNull()) {
            return null;
        } else {
            long timestamp = fieldValue.getTimestampValue();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(getInstantFromMicros(timestamp), ZoneOffset.UTC);
            String formattedTime = dateTimeFormatter.format(zonedDateTime);
            return TextNode.valueOf(formattedTime);
        }
    }
    
    static Instant getInstantFromMicros(Long microsSinceEpoch) {
        return Instant.ofEpochSecond(TimeUnit.MICROSECONDS.toSeconds(microsSinceEpoch),
                                     TimeUnit.MICROSECONDS.toNanos(Math.floorMod(microsSinceEpoch, TimeUnit.SECONDS.toMicros(1))));
    }
}
