package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.testconfig.InitActionConfig;
import ai.aliz.talendtestrunner.testconfig.StepConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ActionConfigForBq {

    public static InitActionConfig getInitActionConfigForBq(Map<String, Object> defaultProperties, Context context, String system, String datasetName, File tableJsonFile) {
        InitActionConfig bqLoadInitActionConfig = new InitActionConfig();
        String tableJsonFileName = tableJsonFile.getName();
        String extension = FilenameUtils.getExtension(tableJsonFileName);
        bqLoadInitActionConfig.setSystem(system);
        String tableName = FilenameUtils.getBaseName(tableJsonFileName);
        bqLoadInitActionConfig.setType("BQLoad");
        Map<String, Object> properties = addBqProperties(datasetName, tableJsonFile, extension, bqLoadInitActionConfig, tableName);
        properties.put("noMetadatAddition", defaultProperties.getOrDefault("init." + context.getId() + ".noMetadatAddition", true));
        return bqLoadInitActionConfig;
    }

    public static AssertActionConfig getAssertActionConfigForBq(Map<String, Object> defaultProperties, String system, String datasetName, File tableDataFile) {
        AssertActionConfig assertActionConfig = new AssertActionConfig();
        String tableName = FilenameUtils.getBaseName(tableDataFile.getName());
        assertActionConfig.setType("AssertDataEquals");
        assertActionConfig.setSystem(system);
        Map<String, Object> properties = addBqProperties(datasetName, tableDataFile, "json", assertActionConfig, tableName);
        properties.put("assertKeyColumns",
                defaultProperties.getOrDefault("assert.assertKeyColumns", Lists.newArrayList(tableName + "_BID", tableName + "_VALID_FROM")));
        properties.put("excludePreviouslyInsertedRows", defaultProperties.getOrDefault("assert.excludePreviouslyInsertedRows", false));
        addAssertProperties(properties, tableDataFile);
        return assertActionConfig;
    }

    private static Map<String, Object> addBqProperties(String datasetName, File tableJsonFile, String extension, StepConfig stepConfig, String tableName) {
        Map<String, Object> properties = stepConfig.getProperties();
        properties.put("sourcePath", tableJsonFile.getAbsolutePath());
        properties.put("dataset", datasetName);
        properties.put("table", tableName);
        properties.put("sourceFormat", extension);
        return properties;
    }

    @SneakyThrows
    private static void addAssertProperties(Map<String, Object> properties, File tableDataFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        String text = new String(Files.readAllBytes(Paths.get(tableDataFile.getPath())), StandardCharsets.UTF_8);
        if (text.contains("assert.properties")) {
            JSONObject object = new JSONObject(text);
            Map<String, Object> property = objectMapper.readValue(object.get("assert.properties").toString(), Map.class);
            property.forEach(properties::putIfAbsent);
        }
    }
}
