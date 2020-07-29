package ai.aliz.talendtestrunner.db;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;

import org.springframework.stereotype.Component;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.service.ExecutorServiceImpl;
import ai.aliz.talendtestrunner.service.InitActionService;
import ai.aliz.talendtestrunner.util.PlaceholderResolver;

@Component
@AllArgsConstructor
@Slf4j
public class BigQueryExecutor implements QueryExecutor {
    
    private PlaceholderResolver placeholderResolver;
    private ExecutorServiceImpl executorService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public void executeScript(String query, Context context) {
        String[] splits = query.split(";");
        List<String> deletes = Lists.newArrayList();
        List<String> inserts = Lists.newArrayList();
        for (String split : splits) {
            String trimmed = split.trim();
            if (!trimmed.isEmpty()) {
                if (trimmed.toUpperCase().contains("DELETE FROM")) {
                    deletes.add(trimmed);
                } else {
                    inserts.add(trimmed);
                }
            }
        }
        
        List<Runnable> deleteRunnables = statementsToRunnables(context, deletes);
        List<Runnable> insertRunnables = statementsToRunnables(context, inserts);
        
        executorService.executeRunnablesInParallel(deleteRunnables, 60, TimeUnit.SECONDS);
        executorService.executeRunnablesInParallel(insertRunnables, 60, TimeUnit.SECONDS);
    }
    
    private List<Runnable> statementsToRunnables(Context context, List<String> statements) {
        List<Runnable> statementRunnables = statements.stream()
                                                      .map(statement -> new Runnable() {
                                                          @Override
                                                          public void run() {
                                                              executeStatement(statement, context);
                                                          }
                                                      })
                                                      .collect(Collectors.toList());
        
        return statementRunnables;
    }
    
    @Override
    public void executeStatement(String query, Context context) {
        String completedQuery = placeholderResolver.resolve(query, context.getParameters());
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(completedQuery).build();
        
        BigQuery bigquery = createBigQueryClient(context);
        try {
            log.info("Executing statement \n{}", completedQuery);
            Iterable<FieldValueList> result = bigquery.query(queryConfig).iterateAll();
            log.debug("Query result {}", result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute statement: \n" + completedQuery, e);
        }
    }
    
    private BigQuery createBigQueryClient(Context context) {
        return BigQueryOptions.newBuilder().setProjectId(context.getParameters().get("project")).build().getService();
    }
    
    public String executeQuery(String query, Context context) {
        
        TableResult queryResult = executeQueryAndGetResult(query, context);
        ArrayNode result = bigQueryResultToJsonArrayNode(queryResult);
        
        return result.toString();
        
    }
    
    public TableResult executeQueryAndGetResult(String query, Context context) {
        String completedQuery = placeholderResolver.resolve(query, context.getParameters());
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
        
        BigQuery bigquery = createBigQueryClient(context);
        try {
            log.info("Executing query {}", query);
            TableResult queryResult = bigquery.query(queryConfig);
            
            return queryResult;
        } catch (Exception e) {
            log.error("Failed to execute query: " + completedQuery, e);
            throw new RuntimeException(e);
        }
    }
    
    public ArrayNode bigQueryResultToJsonArrayNode(TableResult queryResult) {
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
        Iterator<FieldValue> rowIterator = row.iterator();
        while (rowIterator.hasNext()) {
            Field fieldSchema = schemaIterator.next();
            FieldValue fieldValue = rowIterator.next();
            String name = fieldSchema.getName();
            
            BaseJsonNode node = createJsonNode(fieldSchema, fieldValue, recordNode);
        }
        return recordNode;
    }
    
    private BaseJsonNode createJsonNode(Field fieldSchema, FieldValue fieldValue, ObjectNode parent) {
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
                            createJsonNode(arrayElementField, recordValue.get(i), objectNode);
                            i++;
                        }
                        arrayElementNode = objectNode;
                        
                        arrayNode.add(arrayElementNode);
                    }
                } else {
                    throw new UnsupportedOperationException("STRUCT not in an ARRAY is not supported yet. " + fieldSchema);
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
            throw new RuntimeException(String.format("%s %s Failed to parse field \n%s \nwith schema \n%s", fieldType, fieldType.equals(LegacySQLTypeName.RECORD), fieldValue, fieldSchema), e);
        }
        parent.set(fieldSchema.getName(), baseJsonNode);
        
        return baseJsonNode;
        
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
    
    public int insertedRowCount(String tableId, String tableName, Context context) {
        
        TableResult tableResult = executeQueryAndGetResult("SELECT COUNT(*) FROM `" + tableId + "`WHERE " + tableName + "_INSERTED_BY != '" + InitActionService.TEST_INIT + "'", context);
        long count = tableResult.getValues().iterator().next().get(0).getLongValue();
        return (int) count;
    }
    
    public Long getTableLastModifiedAt(Context context, String project, String dataset, String table) {
        BigQuery bigquery = createBigQueryClient(context);
        log.info("Getting last modified at for table: {}.{}.{}", project, dataset, table);
        Table bqTable = bigquery.getTable(TableId.of(project, dataset, table));
        
        return bqTable.getLastModifiedTime();
        
    }
}
