package ai.aliz.jarvis.service.assertaction;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.db.BigQueryExecutor;
import ai.aliz.jarvis.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;

import org.junit.Assert;

import static ai.aliz.talendtestrunner.helper.Helper.DATASET;
import static ai.aliz.talendtestrunner.helper.Helper.PROJECT;
import static ai.aliz.talendtestrunner.helper.Helper.TABLE;
import static ai.aliz.talendtestrunner.helper.Helper.TEST_INIT;

@Service
@Slf4j
public class BqAssertor implements Assertor {

    @Autowired
    TestContextLoader contextLoader;

    public static final String FILTER_CONDITION = "filterCondition";
    
    @Autowired
    private BigQueryExecutor bigQueryExecutor;

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static ObjectMapper formattedMapper = new ObjectMapper();

    static {
        formattedMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private final String BQ_TABLE_ID_TEMPLATE = "%s.%s.%s";
    private static final String UPDATED_AT_PROPERTY_REGEX = "([^\"]+)((_UPDATED_AT)|(_VALID_FROM))";
    private static final Pattern UPDATED_AT_PROPERTY_PATTERN = Pattern.compile(UPDATED_AT_PROPERTY_REGEX);
    private static final String BIGQUERY_UI_TIMESTAMP_REGEX = "([0-9]{4})-([0-9]{2})-([0-9]{2}) ([0-9]{2}):([0-9]{2}):([0-9]{2})(\\.([0-9]+))? UTC";
    private static final Pattern BIGQUERY_UI_TIMESTAMP_PATTERN = Pattern.compile(BIGQUERY_UI_TIMESTAMP_REGEX);
    private static final Pattern BIGQUERY_API_DATETIME_FORMAT = Pattern.compile("([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})(\\.[0-9]+)?Z?");

    private static final DateTimeFormatter BIGQUERY_API_TIMESTAMP_FORMAT = DateTimeFormatter.ISO_INSTANT;
    private Set<String> INEXACT_MATCH_FIELD_PATTERNS = ImmutableSet.of(".*_INSERTED_BY",
            ".*_INSERTED_AT", ".*_SID");

    @Override
    public void doAssert(AssertActionConfig config) {
        assertWithBigQuery(config, contextLoader.getContext(config.getSystem()));
    }

    public void assertTable(String tableId, String expectedJson, Set<String> inexactMatchFields, TestContext context) {
        String selectQuery = String.format("SELECT * FROM %s", tableId);
        String result = bigQueryExecutor.executeQuery(selectQuery, context);

        compareResultsByBusinessRules(expectedJson, result, inexactMatchFields);
    }

    @SneakyThrows
    public void assertTable(AssertActionConfig assertActionConfig, TestContext context) {
        //        String expectedResult = getSourceContent(assertActionConfig);

        String project = context.getParameter("project");
        Map<String, Object> properties = assertActionConfig.getProperties();

        String dataset = TestRunnerUtil.getDatasetName(properties, context);

        String table = (String) properties.get("table");

        String tableId = String.format(BQ_TABLE_ID_TEMPLATE, project, dataset, table);

        //        assertTable(tableId, expectedResult, Collections.emptySet(), context);

        // TODO the field-by-field comparison is not really backward comaptible/production ready
        assertTableFieldByField(assertActionConfig, context);
    }

    public void assertWithBigQuery(AssertActionConfig assertActionConfig, TestContext context) {
        switch (assertActionConfig.getType()) {
            case "AssertDataEquals":
                assertTable(assertActionConfig, context);
                break;
            case "AssertNoChange":
                assertNoChange(assertActionConfig, context);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Not supported assert type %s for context type %s", assertActionConfig.getType(), context.getContextType()));
        }
    }

    private void assertTableFieldByField(AssertActionConfig assertActionConfig, TestContext context) throws IOException {
        String project = context.getParameter("project");
        String dataset = (String) assertActionConfig.getProperties().get("dataset");
        String datasetNamePrefix = context.getParameter("datasetNamePrefix");
        if (datasetNamePrefix != null) {
            dataset = datasetNamePrefix + dataset;
        }
        String table = (String) assertActionConfig.getProperties().get("table");
        String tableId = String.format(BQ_TABLE_ID_TEMPLATE, project, dataset, table);

        String selectQuery = getSelectQuery(assertActionConfig, table, tableId);

        TableResult result = bigQueryExecutor.executeQueryAndGetResult(selectQuery, context);
        Schema schema = result.getSchema();

        ArrayNode actualJson = bigQueryExecutor.bigQueryResultToJsonArrayNode(result);
        ArrayNode expectedJson = objectMapper.readValue(TestRunnerUtil.getSourceContentFromConfigProperties(assertActionConfig), ArrayNode.class);

        Multimap<String, VariablePlaceholder> variablePlaceholderMultimap = ArrayListMultimap.create();
        List<Map<String, Object>> expectedMap = getMapFromJson(tableId, schema, expectedJson, variablePlaceholderMultimap);
        List<Map<String, Object>> actualMap = getMapFromJson(tableId, schema, actualJson, variablePlaceholderMultimap);

        List<String> assertKeyColumns = (List<String>) assertActionConfig.getProperties().get("assertKeyColumns");

        List<String> differences = new ArrayList<>();
        Map<List<Object>, Map<String, Object>> expectedNodeMap = createKeyedMap(expectedMap, assertKeyColumns, differences, false);
        Map<List<Object>, Map<String, Object>> actualNodemap = createKeyedMap(actualMap, assertKeyColumns, differences, true);

        actualMap.forEach(node -> {
            List<Object> key = assertKeyColumns.stream().map(columnName -> node.get(columnName)).collect(Collectors.toList());
            actualNodemap.put(key, node);
        });

        addDifferences(variablePlaceholderMultimap, differences, expectedNodeMap, actualNodemap);

        Assert.assertTrue(differences.stream().collect(Collectors.joining("\n")), differences.isEmpty());
    }

    private void addDifferences(Multimap<String, VariablePlaceholder> variablePlaceholderMultimap, List<String> differences, Map<List<Object>, Map<String, Object>> expectedNodeMap, Map<List<Object>, Map<String, Object>> actualNodemap) {
        log.info("Asserting key-wise matching...");
        Sets.SetView<List<Object>> expectedButNotIntheActual = Sets.difference(expectedNodeMap.keySet(), actualNodemap.keySet());
        if (!expectedButNotIntheActual.isEmpty()) {
            log.warn("Expected but not in the result");
            expectedButNotIntheActual.stream().forEach(key -> {
                log.error(key.toString());
                differences.add("Expected but not in the result: " + key);
            });
        }

        Sets.SetView<List<Object>> notExpectedButIntheActual = Sets.difference(actualNodemap.keySet(), expectedNodeMap.keySet());
        if (!notExpectedButIntheActual.isEmpty()) {
            log.warn("In the result but not expected");
            notExpectedButIntheActual.stream().forEach(key -> {
                log.error(key.toString());
                differences.add("In the result but not expected: " + key);
            });
        }

        log.info("Asserting values in key-wise matching entities...");
        Sets.intersection(actualNodemap.keySet(), expectedNodeMap.keySet())
                .stream()
                .forEach(key -> {

                            Map<String, Object> actual = actualNodemap.get(key);
                            Map<String, Object> expected = expectedNodeMap.get(key);

                            for (Map.Entry<String, Object> entry : expected.entrySet()) {

                                Object actualValue = actual.get(entry.getKey());
                                Object expectedValue = entry.getValue();
                                if (expectedValue instanceof VariablePlaceholder) {
                                    VariablePlaceholder variablePlaceholder = (VariablePlaceholder) expectedValue;
                                    variablePlaceholder.setValue(actualValue);
                                } else if (!Objects.equals(expectedValue, actualValue)) {
                                    String diff = "Diff on entity: " + key + " in column: " + entry.getKey() + "\n expected: " + expectedValue + "\n   actual: " + actualValue;
                                    log.error(diff);
                                    differences.add(diff);
                                }
                            }

                        }

                );
        log.info("Asserting placeholder variables...");
        variablePlaceholderMultimap.keySet().forEach(variableName -> {
            Collection<VariablePlaceholder> variablePlaceholders = variablePlaceholderMultimap.get(variableName);
            boolean allTheSame = variablePlaceholders.stream().distinct().count() <= 1;
            if (!allTheSame) {
                String diff = String.format("The variable values for key {} are different. Values: {}", variableName, variablePlaceholders);
                differences.add(diff);
                log.error(diff);
            }
        });
    }

    private List<Map<String, Object>> getMapFromJson(String tableId, Schema schema, ArrayNode expectedJson, Multimap<String, VariablePlaceholder> variablePlaceholderMultimap) {
        List<Map<String, Object>> expectedMap = new ArrayList<>();
        Iterator<JsonNode> elements = expectedJson.elements();
        while (elements.hasNext()) {
            JsonNode jsonNode = elements.next();
            Map<String, Object> objectMap = convertJsonNodeToObjectMap(tableId, jsonNode, schema.getFields(), variablePlaceholderMultimap);
            expectedMap.add(objectMap);
        }
        return expectedMap;
    }

    private String getSelectQuery(AssertActionConfig assertActionConfig, String table, String tableId) {
        String selectQuery = String.format("SELECT * FROM %s", tableId);
        Map<String, Object> assertProperties = assertActionConfig.getProperties();

        if (Boolean.TRUE.equals(assertProperties.get("excludePreviouslyInsertedRows"))) {
            selectQuery = selectQuery + " WHERE " + table + "_INSERTED_BY != '" + TEST_INIT + "'";
        }

        if (assertProperties.keySet().contains(FILTER_CONDITION)) {
            if (!selectQuery.contains("WHERE")) {
                selectQuery = selectQuery + " WHERE " + assertProperties.get(FILTER_CONDITION);
            } else {
                selectQuery = selectQuery + " AND " + assertProperties.get(FILTER_CONDITION);
            }
        }
        return selectQuery;
    }

    private Map<List<Object>, Map<String, Object>> createKeyedMap(List<Map<String, Object>> objects, List<String> assertKeyColumns, List<String> differences, boolean actual) {
        Map<List<Object>, Map<String, Object>> keyedMap = new HashMap<>();
        objects.forEach(node -> {
            List<Object> key = assertKeyColumns.stream().map(columnName -> node.get(columnName)).collect(Collectors.toList());
            Map<String, Object> previousValue = keyedMap.put(key, node);
            if (previousValue != null) {
                String diff = String.format("There is two %s value for key %s: \n%s, \n%s", actual ? "actual" : "expected", key, previousValue, node);
                log.error(diff);
                differences.add(diff);
            }
        });

        return keyedMap;
    }

    private Map<String, Object> convertJsonNodeToObjectMap(String tableId, JsonNode jsonNode, FieldList fieldList, Multimap<String, VariablePlaceholder> variablePlaceHolderMap) {

        HashMap<String, Object> map = new HashMap<>();



        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            if (INEXACT_MATCH_FIELD_PATTERNS.stream().anyMatch(pattern -> fieldName.matches(pattern))) {
                continue;
            } else if (fieldValue.isNull()) {
                map.put(fieldName, null);
            } else if (fieldValue.isTextual() && VariablePlaceholder.VARIABLE_PLACEHOLDER_PATTERN.matcher(fieldValue.asText()).matches()) {
                Matcher matcher = VariablePlaceholder.VARIABLE_PLACEHOLDER_PATTERN.matcher(fieldValue.asText());
                matcher.matches();
                VariablePlaceholder variablePlaceholder = new VariablePlaceholder();
                variablePlaceholder.setSource(tableId);
                String key = matcher.group(1);
                variablePlaceholder.setKey(key);
                variablePlaceHolderMap.put(key, variablePlaceholder);
            } else {
                Field fieldSchema = fieldList.get(fieldName);
                LegacySQLTypeName type = fieldSchema.getType();
                if (LegacySQLTypeName.STRING.equals(type)) {
                    map.put(fieldName, fieldValue.asText());
                } else if (LegacySQLTypeName.TIMESTAMP.equals(type)) {
                    String timestampString = fieldValue.asText();
                    if (BIGQUERY_UI_TIMESTAMP_PATTERN.matcher(timestampString).matches()) {
                        timestampString = convertLegacyTimestampToAPIFormat(timestampString);
                    }
                    map.put(fieldName, BIGQUERY_API_TIMESTAMP_FORMAT.parse(timestampString, Instant::from));
                } else if (LegacySQLTypeName.BOOLEAN.equals(type)) {
                    map.put(fieldName, Boolean.valueOf(fieldValue.asText()));
                } else if (LegacySQLTypeName.INTEGER.equals(type)) {
                    map.put(fieldName, Integer.valueOf(fieldValue.asText()));
                } else if (LegacySQLTypeName.NUMERIC.equals(type)) {
                    map.put(fieldName, Double.valueOf(fieldValue.asText()));
                } else if (LegacySQLTypeName.DATE.equals(type)) {
                    map.put(fieldName, DateTimeFormatter.ISO_DATE.parse(fieldValue.asText(), LocalDate::from));
                } else if (LegacySQLTypeName.DATETIME.equals(type)) {
                    map.put(fieldName, BIGQUERY_API_TIMESTAMP_FORMAT.parse(fieldValue.asText(), LocalDateTime::from));
                } else if (LegacySQLTypeName.TIME.equals(type)) {
                    map.put(fieldName, DateTimeFormatter.ISO_TIME.parse(fieldValue.asText(), LocalTime::from));
                } else if (LegacySQLTypeName.RECORD.equals(type)) {
                    if (fieldSchema.getMode() == Field.Mode.REPEATED) {
                        List<Map<String, Object>> children = new ArrayList<>();
                        Iterator<JsonNode> childIterator = fieldValue.elements();
                        while (childIterator.hasNext()) {
                            JsonNode child = childIterator.next();
                            Map<String, Object> childObjectMap = convertJsonNodeToObjectMap(tableId, child, fieldSchema.getSubFields(), variablePlaceHolderMap);
                            children.add(childObjectMap);
                        }
                        map.put(fieldSchema.getName(), children);

                    } else {
                        Map<String, Object> childObjectMap = convertJsonNodeToObjectMap(tableId, fieldValue, fieldSchema.getSubFields(), variablePlaceHolderMap);
                        map.put(fieldName, childObjectMap);
                    }

                } else {
                    throw new UnsupportedOperationException("Not supported type: " + type);
                }
            }

        }
        return map;
    }

    @SneakyThrows
    private void compareResultsByBusinessRules(String expectedJson, String actual, Set<String> inexactMatchFields) {
        ArrayNode expectedResultNode = objectMapper.readValue(expectedJson, ArrayNode.class);
        ArrayNode actualJson = objectMapper.readValue(actual, ArrayNode.class);

        tweakFormatToApiBased(expectedResultNode);

        if (expectedResultNode.size() == 0) {
            log.warn("Expected result is empty.");
            Assert.assertTrue("Expected result is empty, actual has items.", actualJson.size() == 0);
        } else {
            diffExpectedResultWithActual(expectedResultNode, actualJson, inexactMatchFields);
        }

    }

    private void diffExpectedResultWithActual(ArrayNode expectedResultNode, ArrayNode actualJson, Set<String> inexactMatchFields) throws JsonProcessingException {
        String orderingProperty = findMainOrderingProperty(expectedResultNode);
        List<String> orderingProperties = findOrderingProperties(expectedResultNode);
        ArrayNode sortedExpected = sortByOrderingProperties(expectedResultNode, orderingProperties);
        ArrayNode sortedActual = sortByOrderingProperties(actualJson, orderingProperties);

        createDebugHelpers(sortedExpected, sortedActual);

        checkRightRecordsExistHumanReadable(orderingProperty, sortedExpected, sortedActual);

        ArrayNode patch = (ArrayNode) JsonDiff.asJson(sortedExpected, sortedActual);

        List<JsonNode> patchNodes = Lists.newArrayList(patch);
        List<JsonNode> acceptableInexactMatches = findAcceptableInexactMatches(patchNodes, sortedExpected, inexactMatchFields);

        if (!acceptableInexactMatches.isEmpty()) {
            log.warn("Found some inexact matches, but they're acceptable: {}", Joiner.on(", ").join(acceptableInexactMatches));
        }

        List<JsonNode> problematicPatchNodes = Lists.newArrayList(patchNodes);
        problematicPatchNodes.removeAll(acceptableInexactMatches);

        StringBuilder problematicDiffResultHumanReadable = prepareHumanReadableDiffFromPatch(sortedExpected, problematicPatchNodes);

        Assert.assertTrue("Comparison has the following problems in human-readable format:\n" + problematicDiffResultHumanReadable
                        + "\n patch: " + patch,
                problematicPatchNodes.isEmpty());
    }

    @SneakyThrows
    private void createDebugHelpers(ArrayNode sortedExpected, ArrayNode sortedActual) throws JsonProcessingException {

        Path expectedFile = createTempFile();
        Path actualFile = createTempFile();

        Files.write(expectedFile, formattedMapper.writeValueAsBytes(sortedExpected));
        Files.write(actualFile, formattedMapper.writeValueAsBytes(sortedActual));
        log.info("Diff command:\nmeld {} {}", expectedFile, actualFile);
        log.info("Sorted expected row: ------------------ \n{}\nSorted actual row: ------------------- \n{}",
                objectMapper.writeValueAsString(sortedExpected), objectMapper.writeValueAsString(sortedActual));
    }

    private Path createTempFile() throws IOException {
        return Files.createTempFile("temp", null);
    }

    private List<JsonNode> findAcceptableInexactMatches(List<JsonNode> patchNodes, ArrayNode expectedResultNode,
                                                        Set<String> inexactMatchFields) {
        List<JsonNode> allAcceptableDifferences = Lists.newArrayList();

        allAcceptableDifferences.addAll(findDifferencesGloballyMarkedInexact(patchNodes, expectedResultNode));
        allAcceptableDifferences.addAll(findDifferencesMarkedInexactForTable(patchNodes, expectedResultNode, inexactMatchFields));
        allAcceptableDifferences.addAll(findLegacyBooleanDifferences(patchNodes, expectedResultNode));

        return allAcceptableDifferences;
    }

    private List<JsonNode> findDifferencesMarkedInexactForTable(List<JsonNode> patchNodes, ArrayNode expectedResultNode,
                                                                Set<String> inexactMatchFields) {
        return findAcceptableInexactDifferences(patchNodes, expectedResultNode,
                nodeName -> inexactMatchFields.stream().anyMatch(nodeName::contains));
    }

    /**
     * Necessary because BQ UI apparently used to print json results with boolean values enclosed between double quotation
     * marks, but it doesn't seem to use them anymore.
     */
    private List<JsonNode> findLegacyBooleanDifferences(List<JsonNode> patchNodes, ArrayNode expectedResultNode) {
        return patchNodes.stream()
                .filter(node -> isLegacyBooleanDifference(node, expectedResultNode))
                .collect(Collectors.toList());
    }

    private Boolean isLegacyBooleanDifference(JsonNode patchNode, ArrayNode expectedResultNode) {
        if (isReplacement(patchNode)) {
            String valueForPath = findNodeForPath(expectedResultNode, patchNode.get("path").asText()).asText();
            if ("true".equals(valueForPath) || "false".equals((valueForPath))) {
                JsonNode patchValue = patchNode.get("value");
                return valueForPath.equals(patchValue.asText());
            }
        }
        return false;
    }

    /**
     * This finds differences belonging to fields marked for inexact match, because of changing between each run,
     * for example because of being a generated value (*_SID), or containing current date (*_INSERTED_AT)
     */
    private List<JsonNode> findDifferencesGloballyMarkedInexact(List<JsonNode> patchNodes, ArrayNode expectedResultNode) {
        Joiner regexOrOperatorJoiner = Joiner.on(")|(");
        String inexactMatchAcceptableRegex = "(" + regexOrOperatorJoiner.join(INEXACT_MATCH_FIELD_PATTERNS) + ")";
        Pattern inexactMatchAcceptablePattern = Pattern.compile(inexactMatchAcceptableRegex);
        return findAcceptableInexactDifferences(patchNodes, expectedResultNode,
                nodeName -> inexactMatchAcceptablePattern.matcher(nodeName).matches());
    }

    private List<JsonNode> findAcceptableInexactDifferences(List<JsonNode> patchNodes, ArrayNode expectedResultNode,
                                                            Predicate<String> eligibleForInexactComparison) {
        return patchNodes.stream()
                .filter(node -> {
                    if (!node.get("op").asText().equals("replace")) {
                        return false;
                    }
                    String nodeName = node.get("path").asText();
                    boolean inexactMatchEnough = eligibleForInexactComparison.test(nodeName);
                    String expectedNodeForField = findNodeForPath(expectedResultNode, nodeName).asText();
                    String actualNodeValue = node.get("value").asText();
                    if (inexactMatchEnough) {
                        // for an inexact match we only check whether the nullity of expected and actual are the same
                        return (expectedNodeForField == null) == (actualNodeValue == null);
                    } else if (identicatTimestampsInPractice(expectedNodeForField, actualNodeValue)) {
                        return true;
                    } else if (identicalNumbersInPractice(expectedNodeForField, actualNodeValue)) {
                        return true;
                    } else {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    private Pattern NUMERIC_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    public boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        return NUMERIC_PATTERN.matcher(strNum).matches();
    }

    private boolean identicalNumbersInPractice(String expectedNodeForField, String actualNodeValue) {
        if (isNumeric(expectedNodeForField) && isNumeric(actualNodeValue)) {
            BigDecimal expected = new BigDecimal(expectedNodeForField);
            BigDecimal actual = new BigDecimal(actualNodeValue);
            return expected.compareTo(actual) == 0;
        } else {
            return false;
        }
    }

    private boolean identicatTimestampsInPractice(String expectedNodeForField, String actualNodeValue) {
        if (BIGQUERY_API_DATETIME_FORMAT.matcher(expectedNodeForField).matches()
                && BIGQUERY_API_DATETIME_FORMAT.matcher(actualNodeValue).matches()) {
            // are they equal with trailing zeros removed
            return expectedNodeForField.replaceAll("0*Z?$", "")
                    .equals(actualNodeValue.replaceAll("0*Z?$", ""));
        } else {
            return false;
        }
    }

    private StringBuilder prepareHumanReadableDiffFromPatch(ArrayNode sortedExpected, List<JsonNode> patch) {
        StringBuilder diffResultHumanReadable = new StringBuilder();
        for (JsonNode jsonNode : patch) {
            if (isReplacement(jsonNode)) {
                String path = jsonNode.get("path").asText();
                JsonNode expectedResult = findNodeForPath(sortedExpected, path);
                diffResultHumanReadable.append(
                        String.format("%s\n\tExp:%s\n\tAct:%s\n", path, expectedResult.asText(), jsonNode.get("value").asText()));
            }
        }
        return diffResultHumanReadable;
    }

    private boolean isReplacement(JsonNode jsonNode) {
        return "replace".equals(jsonNode.get("op").asText());
    }

    private JsonNode findNodeForPath(ArrayNode sortedExpected, String path) {
        String[] pathParts = path.substring(1).split("/");
        return findNodeForPath(sortedExpected, pathParts, 0);
    }

    private JsonNode findNodeForPath(JsonNode expectedResultNode, String[] path, int level) {
        if (level == path.length) {
            return expectedResultNode;
        }

        String nextPathItem = path[level];
        if (expectedResultNode instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) expectedResultNode;
            int arrayIndex = Integer.parseInt(nextPathItem);
            return findNodeForPath(arrayNode.get(arrayIndex), path, level + 1);
        } else {
            return findNodeForPath(expectedResultNode.get(nextPathItem), path, level + 1);
        }
    }

    private void tweakFormatToApiBased(ArrayNode actualJson) {
        for (JsonNode node : actualJson) {
            Iterator<Map.Entry<String, JsonNode>> fieldsIter = node.fields();
            while (fieldsIter.hasNext()) {
                Map.Entry<String, JsonNode> entry = fieldsIter.next();
                if (entry.getValue().isTextual()) {
                    String fieldValue = entry.getValue().asText();
                    if (BIGQUERY_UI_TIMESTAMP_PATTERN.matcher(fieldValue).matches()) {
                        String timestampWithCorrectedFormat = convertLegacyTimestampToAPIFormat(fieldValue);
                        entry.setValue(TextNode.valueOf(timestampWithCorrectedFormat));
                    }
                }
            }
        }
    }

    private String convertLegacyTimestampToAPIFormat(String fieldValue) {
        return fieldValue.replace(" UTC", "Z").replace(" ", "T");
    }

    private void checkRightRecordsExistHumanReadable(String orderingProperty, ArrayNode sortedExpected, ArrayNode sortedActual) {
        Multimap<String, JsonNode> expectedRecordsByOrderingProp = collectRecordsByOrderingProp(sortedExpected, orderingProperty);
        Multimap<String, JsonNode> actualRecordsByOrderingProp = collectRecordsByOrderingProp(sortedActual, orderingProperty);

        Set<String> allTimestamps = Sets.union(expectedRecordsByOrderingProp.keySet(), actualRecordsByOrderingProp.keySet());

        List<String> errors = Lists.newArrayList();

        for (String timestamp : allTimestamps) {
            if (expectedRecordsByOrderingProp.containsKey(timestamp) && !actualRecordsByOrderingProp.containsKey(timestamp)) {
                errors.add("Expected record(s) missing for timestamp: " + timestamp + " : " + expectedRecordsByOrderingProp.get(timestamp));
            } else if (!expectedRecordsByOrderingProp.containsKey(timestamp) && actualRecordsByOrderingProp.containsKey(timestamp)) {
                errors.add("Unexpected record(s) with timestamp: " + timestamp + " : " + actualRecordsByOrderingProp.get(timestamp));
            } else {
                int expectedCount = expectedRecordsByOrderingProp.get(timestamp).size();
                int actualCount = actualRecordsByOrderingProp.get(timestamp).size();
                if (expectedCount != actualCount) {
                    errors.add(String.format("Expected and actual count don't match for timestamp %s, expected %s, got %s",
                            timestamp, expectedCount, actualCount));
                }
            }
        }

        String errorsJoined = Joiner.on(", ").join(errors);
        Assert.assertTrue("Expected and actual records don't match. Errors: " + errorsJoined, errors.isEmpty());
    }

    private Multimap<String, JsonNode> collectRecordsByOrderingProp(ArrayNode records, String orderingProperty) {
        Multimap<String, JsonNode> expectedRecordsByOrderingProp = ArrayListMultimap.create();
        for (JsonNode record : records) {
            expectedRecordsByOrderingProp.put(record.get(orderingProperty).asText(), record);
        }

        return expectedRecordsByOrderingProp;
    }

    private ArrayNode sortByOrderingProperties(ArrayNode inputArray, List<String> orderingProperties) {
        Preconditions.checkArgument(!orderingProperties.isEmpty(), "At least one ordering property is mandatory.");
        Comparator<JsonNode> comparator = Comparator.comparing((JsonNode node) -> node.get(orderingProperties.get(0)).asText());
        for (String property : orderingProperties.subList(1, orderingProperties.size())) {
            comparator = comparator.thenComparing((JsonNode node) -> node.get(property).asText());
        }

        ArrayList<JsonNode> nodeList = Lists.newArrayList(inputArray);

        Collections.sort(nodeList, comparator);

        ArrayNode result = objectMapper.createArrayNode();
        for (JsonNode record : nodeList) {
            result.add(record);
        }

        return result;
    }

    private List<String> findOrderingProperties(ArrayNode expectedResultNode) {
        String mainOrderingProperty = findMainOrderingProperty(expectedResultNode);

        Set<String> exactProperties = findPropertiesByPredicate(expectedResultNode,
                propertyName -> !INEXACT_MATCH_FIELD_PATTERNS.stream().anyMatch(pattern -> propertyName.matches(pattern)));
        exactProperties.remove(mainOrderingProperty);

        List<String> orderingProperties = Lists.newArrayList();
        orderingProperties.add(mainOrderingProperty);
        orderingProperties.addAll(exactProperties);

        return orderingProperties;
    }

    private String findMainOrderingProperty(ArrayNode expectedResultNode) {
        return findPropertyMatchingPattern(expectedResultNode, UPDATED_AT_PROPERTY_PATTERN);
    }

    private String findPropertyMatchingPattern(ArrayNode expectedResultNode, Pattern pattern) {
        Set<String> propertyCandidates = findPropertiesByPredicate(expectedResultNode, propertyName -> pattern.matcher(propertyName).matches());

        Preconditions.checkArgument(propertyCandidates.size() == 1, String.format("Should have found" +
                "1 property candidate, but found " + propertyCandidates.size()));

        return propertyCandidates.iterator().next();
    }

    private Set<String> findPropertiesByPredicate(ArrayNode expectedResultNode, Predicate<String> propertyPredicate) {
        JsonNode firstResultItem = expectedResultNode.get(0);
        Iterator<Map.Entry<String, JsonNode>> fields = firstResultItem.fields();
        Set<String> propertyCandidates = Sets.newHashSet();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> recordProp = fields.next();
            String propertyName = recordProp.getKey();
            if (propertyPredicate.test(propertyName)) {
                propertyCandidates.add(recordProp.getKey());
            }
        }
        return propertyCandidates;
    }

    public void assertNoChange(AssertActionConfig assertActionConfig, TestContext context) {
        String project = context.getParameter(PROJECT);
        Map<String, Object> properties = assertActionConfig.getProperties();
        String dataset = (String) properties.get(DATASET);
        String datasetNamePrefix = context.getParameter("datasetNamePrefix");
        if (datasetNamePrefix != null) {
            dataset = datasetNamePrefix + dataset;
        }
        String table = (String) properties.get(TABLE);

        String tableId = String.format(BQ_TABLE_ID_TEMPLATE, project, dataset, table);

        int insertedRowCount = bigQueryExecutor.insertedRowCount(tableId, table, context);

        if (insertedRowCount > 0) {
            log.warn("No added rows expected, but there are {} new rows", insertedRowCount);
        }

    }

    @Data
    private static class VariablePlaceholder {

        private static Pattern VARIABLE_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.*)\\}\\}");
        private String source;
        private String key;
        private Object value;
    }
}
