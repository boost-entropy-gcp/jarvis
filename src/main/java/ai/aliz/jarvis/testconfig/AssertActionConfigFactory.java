package ai.aliz.jarvis.testconfig;

import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.util.JarvisUtil;

import static ai.aliz.talendtestrunner.helper.Helper.DATASET;
import static ai.aliz.talendtestrunner.helper.Helper.SOURCE_FORMAT;
import static ai.aliz.talendtestrunner.helper.Helper.SOURCE_PATH;
import static ai.aliz.talendtestrunner.helper.Helper.TABLE;

@Service
public class AssertActionConfigFactory {
    
    @Autowired
    private TestContextLoader contextLoader;
    
    public List<AssertActionConfig> getAssertActionConfigs(Map<String, Object> defaultProperties, File testCaseFolder) {
        Path assertFolder = JarvisUtil.getTargetFolderPath(testCaseFolder, "assert");
        List<AssertActionConfig> assertActionConfigs = null;
        try {
            assertActionConfigs = Files.list(assertFolder).flatMap(assertActionConfigPath -> {
                List<AssertActionConfig> assertActionConfigsForFolder = new ArrayList<>();
                if (Files.isDirectory(assertActionConfigPath)) {
                    
                    File assertContextFolder = assertActionConfigPath.toFile();
                    String directoryName = assertContextFolder.getName();
                    TestContext context = JarvisUtil.getContext(contextLoader, directoryName);
                    String system = context.getId();
                    
                    switch (context.getContextType()) {
                        case BigQuery:
                            for (File datasetFolder : assertContextFolder.listFiles()) {
                                Preconditions.checkArgument(Files.isDirectory(datasetFolder.toPath()),
                                                            "The context folder should contain only directories for the different datasets: %s",
                                                            datasetFolder);
                                String datasetName = datasetFolder.getName();
                                for (File tableDataFile : datasetFolder.listFiles()) {
                                    Preconditions.checkArgument(Files.isRegularFile(tableDataFile.toPath()),
                                                                "The dataset folder should contain only json files for the different tables: %s",
                                                                tableDataFile);
                                    assertActionConfigsForFolder.add(getAssertActionConfigForBq(defaultProperties, system, datasetName, tableDataFile));
                                }
                            }
                            break;
                        case MySQL:
                            for (File tableDataFile : assertContextFolder.listFiles()) {
                                Preconditions.checkArgument(Files.isRegularFile(tableDataFile.toPath()),
                                                            "The assert folder should contain only json files for the different tables: %s",
                                                            tableDataFile);
                                assertActionConfigsForFolder.add(getAssertActionConfigForMySQL(system, tableDataFile));
                            }
                            break;
                        default:
                            throw new UnsupportedOperationException("Not supported context type for assert " + context.getContextType());
                    }
                } else {
                    throw new UnsupportedOperationException("Files are not supported in assert autodetect: " + assertActionConfigPath);
                }
                
                return assertActionConfigsForFolder.stream();
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return assertActionConfigs;
    }
    
    private static AssertActionConfig getAssertActionConfigForMySQL(String system, File tableDataFile) {
        AssertActionConfig assertActionConfig = new AssertActionConfig();
        assertActionConfig.setType("AssertTalendJobState");
        assertActionConfig.setSystem(system);
        assertActionConfig.getProperties().put(SOURCE_PATH, tableDataFile.getAbsolutePath());
        return assertActionConfig;
    }
    
    public AssertActionConfig getAssertActionConfigForBq(Map<String, Object> defaultProperties, String system, String datasetName, File tableDataFile) {
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
    
    @SneakyThrows
    private void addAssertProperties(Map<String, Object> properties, File tableDataFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        String text = new String(Files.readAllBytes(Paths.get(tableDataFile.getPath())), StandardCharsets.UTF_8);
        if (text.contains("assert.properties")) {
            JSONObject object = new JSONObject(text);
            Map<String, Object> property = objectMapper.readValue(object.get("assert.properties").toString(), Map.class);
            property.forEach(properties::putIfAbsent);
        }
    }
    
    private static Map<String, Object> addBqProperties(String datasetName, File tableJsonFile, String extension, StepConfig stepConfig, String tableName) {
        Map<String, Object> properties = stepConfig.getProperties();
        properties.put(SOURCE_PATH, tableJsonFile.getAbsolutePath());
        properties.put(DATASET, datasetName);
        properties.put(TABLE, tableName);
        properties.put(SOURCE_FORMAT, extension);
        return properties;
    }
    
}
