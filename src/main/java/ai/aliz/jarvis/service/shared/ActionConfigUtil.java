package ai.aliz.jarvis.service.shared;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import ai.aliz.jarvis.config.AssertActionConfig;
import ai.aliz.jarvis.config.InitActionConfig;
import ai.aliz.jarvis.config.InitActionType;
import ai.aliz.jarvis.config.StepConfig;

import static ai.aliz.jarvis.util.JarvisConstants.ASSERT_KEY_COLUMNS;
import static ai.aliz.jarvis.util.JarvisConstants.DATASET;
import static ai.aliz.jarvis.util.JarvisConstants.EXCLUDE_PREVIOUSLY_INSERTED_ROWS;
import static ai.aliz.jarvis.util.JarvisConstants.JSON_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.NO_METADAT_ADDITION;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_PATH;
import static ai.aliz.jarvis.util.JarvisConstants.TABLE;

@UtilityClass
public class ActionConfigUtil {
    
    //BigQuery
    
    public static InitActionConfig getInitActionConfigForBq(Map<String, Object> defaultProperties, String contextId, String system, String datasetName, File tableJsonFile) {
        InitActionConfig bqLoadInitActionConfig = new InitActionConfig();
        bqLoadInitActionConfig.setSystem(system);
        bqLoadInitActionConfig.setType(InitActionType.BQLoad);
        
        String tableJsonFileName = tableJsonFile.getName();
        String extension = FilenameUtils.getExtension(tableJsonFileName);
        String tableName = FilenameUtils.getBaseName(tableJsonFileName);
        Map<String, Object> properties = addBqProperties(datasetName, tableJsonFile, extension, bqLoadInitActionConfig, tableName);
        properties.put(NO_METADAT_ADDITION, defaultProperties.getOrDefault("init." + contextId + "." + NO_METADAT_ADDITION, true));
        return bqLoadInitActionConfig;
    }
    
    public static AssertActionConfig getAssertActionConfigForBq(Map<String, Object> defaultProperties, String system, String datasetName, File tableDataFile) {
        AssertActionConfig assertActionConfig = new AssertActionConfig();
        assertActionConfig.setSystem(system);
        assertActionConfig.setType("AssertDataEquals");
        
        String tableName = FilenameUtils.getBaseName(tableDataFile.getName());
        Map<String, Object> properties = addBqProperties(datasetName, tableDataFile, JSON_FORMAT, assertActionConfig, tableName);
        properties.put(ASSERT_KEY_COLUMNS, defaultProperties.getOrDefault("assert." + ASSERT_KEY_COLUMNS, Lists.newArrayList(tableName + "_BID", tableName + "_VALID_FROM")));
        properties.put(EXCLUDE_PREVIOUSLY_INSERTED_ROWS, defaultProperties.getOrDefault("assert." + EXCLUDE_PREVIOUSLY_INSERTED_ROWS, false));
        addAssertProperties(properties, tableDataFile);
        return assertActionConfig;
    }
    
    private static Map<String, Object> addBqProperties(String datasetName, File tableJsonFile, String extension, StepConfig stepConfig, String tableName) {
        Map<String, Object> properties = stepConfig.getProperties();
        properties.put(SOURCE_PATH, tableJsonFile.getAbsolutePath());
        properties.put(DATASET, datasetName);
        properties.put(TABLE, tableName);
        properties.put(SOURCE_FORMAT, extension);
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
    
    //SFTP
    
    public static InitActionConfig getInitActionConfigForSFTP(Path initActionFile, String system) {
        InitActionConfig initActionConfig = new InitActionConfig();
        initActionConfig.setType(InitActionType.SFTPLoad);
        initActionConfig.setSystem(system);
        initActionConfig.getProperties().put(SOURCE_PATH, initActionFile.toFile().getAbsolutePath());
        return initActionConfig;
    }
}