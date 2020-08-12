package ai.aliz.talendtestrunner.testconfig;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.apache.commons.io.FilenameUtils;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.context.ContextType;

@Data
@ToString(exclude = "parentSuite")
public class TestSuite {
    
    public static final String TEST_SUITE_FILE_NAME = "testSuite.json";
    public static final String EXECUTIONS_KEY = "executions";
    public static final String DEFAULT_PROPERTIES_KEY = "defaultProperties";
    public static final String PROPERTIES_KEY = "properties";
    public static final String CASE_AUTO_DETECT_KEY = "caseAutoDetect";
    public static final String TEST_CASES_KEY = "testCases";

    private Boolean caseAutoDetect;

    private final List<TestCase> testCases = new ArrayList<>();
    
    private String configPath;
    
    private String rootFolder;
    
    private Map<String, Object> properties;
    
    private TestSuite parentSuite;
    
    private final List<TestSuite> suites = new ArrayList<>();
    
    public List<TestCase> listTestCases() {
        return listTestCases(this);
    }
    
    private List<TestCase> listTestCases(TestSuite suite) {
        System.out.println(suite);
        List<TestCase> testCases = new ArrayList<>();
        
        testCases.addAll(suite.getTestCases());
        
        for (TestSuite testSuite : suite.getSuites()) {
            testCases.addAll(listTestCases(testSuite));
        }
        return testCases;
    }
    
    @SneakyThrows
    public static TestSuite readTestConfig(String testSuitePath, ContextLoader contextLoader) {
        
        File testConfigFile = new File(testSuitePath);
        Preconditions.checkArgument(testConfigFile.isDirectory(), "Testconfig folder %s is not a directory", testSuitePath);
        TestSuite parentSuite;
        Path testSuiteConfigFilePath = Paths.get(testSuitePath, TEST_SUITE_FILE_NAME);
        if (testSuiteConfigFilePath.toFile().exists()) {
            parentSuite = parseFromJson(testSuiteConfigFilePath.toFile(), null, contextLoader);
        } else {
            parentSuite = new TestSuite();
            String descriptorFolder = testSuitePath.endsWith(File.separator) ? testSuitePath : testSuitePath + File.separator;
            parentSuite.setRootFolder(descriptorFolder);
        }
        Path descriptorFolder = testConfigFile.toPath();
        
        Files.list(descriptorFolder).filter(path -> Files.isDirectory(path)).forEach(path -> processFolder(path, parentSuite, contextLoader));
        
        return parentSuite;
    }
    
    @SneakyThrows
    private static void processFolder(Path folder, final TestSuite parentSuite, ContextLoader contextLoader) {
        Preconditions.checkArgument(Files.isDirectory(folder));
        
        TestSuite currentSuit = parentSuite;
        File descriptorFile = Paths.get(folder.toString(), TEST_SUITE_FILE_NAME).toFile();
        if (descriptorFile.exists()) {
            currentSuit = parseFromJson(descriptorFile, parentSuite, contextLoader);
        }
        
        final TestSuite newParent = currentSuit;
        if(currentSuit.getTestCases().isEmpty()) {
            Files.list(folder).filter(path -> Files.isDirectory(path)).forEach(path -> processFolder(path, newParent, contextLoader));
        }
        
        return;
    }
    
    public static TestSuite parseFromJson(File testConfigFile, TestSuite parentSuite, ContextLoader contextLoader) {
        
        Gson gson = new Gson();
        TestCase testCase = new TestCase();
        TestSuite testSuite = new TestSuite();
        testSuite.setParentSuite(parentSuite);
        if (parentSuite != null) {
            parentSuite.getSuites().add(testSuite);
        }
        testSuite.setConfigPath(testConfigFile.getAbsolutePath());
        String descriptorFolder = testConfigFile.getParentFile().getAbsolutePath() + File.separator;
        
        testSuite.setRootFolder(descriptorFolder);
        try (FileReader fileReader = new FileReader(testConfigFile)) {
            Map testSuiteMap = gson.fromJson(fileReader, Map.class);
            
            testSuite.setProperties((Map<String, Object>) testSuiteMap.get(PROPERTIES_KEY));
            
            Boolean caseAutoDetect = (Boolean) testSuiteMap.get(CASE_AUTO_DETECT_KEY);
            testSuite.setCaseAutoDetect(caseAutoDetect);
            
            Map<String, Object> defaultProperties = (Map<String, Object>) testSuiteMap.getOrDefault(DEFAULT_PROPERTIES_KEY, new HashMap<>());

            if (testSuiteMap.get(EXECUTIONS_KEY) != null) {
                List<Map<String, String>> executionActions = (List<Map<String, String>>) testSuiteMap.getOrDefault(EXECUTIONS_KEY, Collections.singletonMap("type", "noOps"));
                List<ExecutionActionConfig> executionActionConfigs = getExecutionActionConfigs(contextLoader, executionActions);
                testCase.getExecutionActionConfigs().addAll(executionActionConfigs);
            }

            if (Boolean.TRUE.equals(caseAutoDetect)) {
                List<TestCase> testCases = Files.list(Paths.get(descriptorFolder)).filter(Files::isDirectory).map(path -> {
                    File testCaseFolder = path.toFile();
                    testCase.setPath(testCaseFolder.getAbsolutePath());
                    testCase.setName(testCaseFolder.getName());


                    List<InitActionConfig> initActions = getInitActionConfigs(contextLoader, defaultProperties, testCaseFolder);
                    testCase.getInitActionConfigs().addAll(initActions);

                    List<AssertActionConfig> assertActionConfigs = getAssertActionConfigs(contextLoader, defaultProperties, testCaseFolder);
                    testCase.getAssertActionConfigs().addAll(assertActionConfigs);
                    
                    return testCase;
                }).collect(Collectors.toList());
                
                testSuite.getTestCases().addAll(testCases);
                
            }
            
            List<Map<String, Object>> testCases = (List<Map<String, Object>>) testSuiteMap.get(TEST_CASES_KEY);
            if (testCases != null) {
                setTestCases(testSuite, descriptorFolder, testCases);
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return testSuite;
        
    }

    private static List<ExecutionActionConfig> getExecutionActionConfigs(ContextLoader contextLoader, List<Map<String, String>> executions) {
        List<ExecutionActionConfig> executionActionConfigs = Lists.newArrayList();

        String repositoryRoot = moderateFilePathSlashes(contextLoader.getContext("local").getParameter("repositoryRoot"));
        return executions.stream()
                .flatMap(e -> {
                    ExecutionActionConfig executionActionConfig = new ExecutionActionConfig();
                    executionActionConfig.setType(ExecutionType.valueOf(checkExecutionType(e.get("executionType"))));
                    executionActionConfig.getProperties().put("sourcePath", repositoryRoot + e.get("queryPath"));
                    executionActionConfigs.add(executionActionConfig);

                    return executionActionConfigs.stream();
                })
                .collect(Collectors.toList());
    }

    private static void setTestCases(TestSuite testSuite, String descriptorFolder, List<Map<String, Object>> testCases) {
        for (Map<String, Object> testCaseMap : testCases) {
            TestCase testCase = new TestCase();
            String caseName = (String) testCaseMap.get("name");
            testCase.setName(caseName);

            List<Map<String, Object>> initActions = (List<Map<String, Object>>) testCaseMap.get("initActions");
            for (Map<String, Object> initActionMap : initActions) {
                InitActionConfig initActionConfig = new InitActionConfig();
                initActionConfig.setSystem((String) initActionMap.remove("system"));
                initActionConfig.setType((String) initActionMap.remove("type"));
                initActionConfig.setDescriptorFolder(descriptorFolder + caseName + File.separator);
                initActionConfig.getProperties().putAll(initActionMap);

                testCase.getInitActionConfigs().add(initActionConfig);
            }

            List<Map<String, Object>> assertActions = (List<Map<String, Object>>) testCaseMap.get("assertActions");
            for (Map<String, Object> assertActionMap : assertActions) {
                final String type = (String) assertActionMap.get("type");

                AssertActionConfig assertActionConfig = new AssertActionConfig();
                assertActionConfig.setSystem((String) assertActionMap.remove("system"));
                assertActionConfig.setType(type);
                assertActionConfig.setDescriptorFolder(descriptorFolder + caseName + File.separator);
                assertActionConfig.setProperties(assertActionMap);
                testCase.getAssertActionConfigs().add(assertActionConfig);
            }

            testSuite.getTestCases().add(testCase);
        }
    }

    private static List<AssertActionConfig> getAssertActionConfigs(ContextLoader contextLoader, Map<String, Object> defaultProperties, File testCaseFolder) {
        Path assertFolder = getTargetFolderPath(testCaseFolder, "assert");
        List<AssertActionConfig> assertActionConfigs = null;
        try {
            assertActionConfigs = Files.list(assertFolder).flatMap(assertActionConfigPath -> {
                List<AssertActionConfig> assertActionConfigsForFolder = new ArrayList<>();
                if (Files.isDirectory(assertActionConfigPath)) {

                    File assertContextFolder = assertActionConfigPath.toFile();
                    String directoryName = assertContextFolder.getName();
                    Context context = getContext(contextLoader, directoryName);
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
                                    String tableName = FilenameUtils.getBaseName(tableDataFile.getName());
                                    AssertActionConfig assertActionConfig = new AssertActionConfig();
                                    assertActionConfig.setType("AssertDataEquals");
                                    assertActionConfig.setSystem(system);
                                    Map<String, Object> properties = addBqProperties(datasetName, tableDataFile, "json", assertActionConfig, tableName);
                                    properties.put("assertKeyColumns",
                                                   defaultProperties.getOrDefault("assert.assertKeyColumns", Lists.newArrayList(tableName + "_BID", tableName + "_VALID_FROM")));
                                    properties.put("excludePreviouslyInsertedRows", defaultProperties.getOrDefault("assert.excludePreviouslyInsertedRows", false));
                                    assertActionConfigsForFolder.add(assertActionConfig);
                                }
                            }
                            break;
                        case MySQL:
                            for (File tableDataFile : assertContextFolder.listFiles()) {
                                Preconditions.checkArgument(Files.isRegularFile(tableDataFile.toPath()),
                                                            "The assert folder should contain only json files for the different tables: %s",
                                                            tableDataFile);
                                AssertActionConfig assertActionConfig = new AssertActionConfig();
                                assertActionConfig.setType("AssertTalendJobState");
                                assertActionConfig.setSystem(system);
                                assertActionConfig.getProperties().put("sourcePath", tableDataFile.getAbsolutePath());
                                assertActionConfigsForFolder.add(assertActionConfig);

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

    private static List<InitActionConfig> getInitActionConfigs(ContextLoader contextLoader, Map<String, Object> defaultProperties, File testCaseFolder) {
        Path preFolder =getTargetFolderPath(testCaseFolder, "pre");
        List<InitActionConfig> initActions = null;
        try {
            initActions = Files.list(preFolder).flatMap(initActionFile -> {

                List<InitActionConfig> initActionConfigs = Lists.newArrayList();

                String fileName = initActionFile.toFile().getName();
                if (Files.isRegularFile(initActionFile)) {
                    InitActionConfig initActionConfig = new InitActionConfig();
                    String baseName = FilenameUtils.getBaseName(fileName);
                    Preconditions.checkNotNull(contextLoader.getContext(baseName), "No context exists with name: %s", baseName);

                    initActionConfig.setSystem(baseName);

                    String extension = FilenameUtils.getExtension(fileName);

                    initActionConfig.setDescriptorFolder(null);

                    switch (extension) {
                        case "bql":
                        case "sql":
                            initActionConfig.setType("SQLExec");
                            initActionConfig.getProperties().put("sourcePath", initActionFile.toFile().getAbsolutePath());
                            break;
                        default:
                            throw new UnsupportedOperationException("Not supported extension for init action autodetect: " + initActionFile);

                    }
                    initActionConfigs.add(initActionConfig);
                } else {
                    Context context = getContext(contextLoader, fileName);
                    String system = fileName;

                    ContextType contextType = context.getContextType();
                    switch (contextType) {
                        case SFTP:
                            InitActionConfig initActionConfig = new InitActionConfig();
                            initActionConfig.setSystem(system);
                            initActionConfig.setType("SFTPLoad");
                            initActionConfig.getProperties().put("sourcePath", initActionFile.toFile().getAbsolutePath());
                            break;
                        case BigQuery:
                            for (File datasetFolder : initActionFile.toFile().listFiles()) {
                                Preconditions.checkArgument(datasetFolder.isDirectory(), "%s is not a directory", datasetFolder);
                                String datasetName = datasetFolder.getName();
                                for (File tableJsonFile : datasetFolder.listFiles()) {
                                    Preconditions.checkArgument(tableJsonFile.isFile());
                                    String tableJsonFileName = tableJsonFile.getName();
                                    String extension = FilenameUtils.getExtension(tableJsonFileName);
                                    InitActionConfig bqLoadInitActionConfig = new InitActionConfig();
                                    bqLoadInitActionConfig.setSystem(system);
                                    String tableName = FilenameUtils.getBaseName(tableJsonFileName);
                                    bqLoadInitActionConfig.setType("BQLoad");
                                    Map<String, Object> properties = addBqProperties(datasetName, tableJsonFile, extension, bqLoadInitActionConfig, tableName);
                                    properties.put("noMetadatAddition", defaultProperties.getOrDefault("init." + context.getId() + ".noMetadatAddition", true));

                                    initActionConfigs.add(bqLoadInitActionConfig);
                                }
                            }
                            break;
                        default:
                            throw new UnsupportedOperationException(String.format("Not supported context type %s for folder %s", contextType, fileName));
                    }

                }

                return initActionConfigs.stream();
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return initActions;
    }

    private static Context getContext(ContextLoader contextLoader, String fileName) {
        Context context = contextLoader.getContext(fileName);
        Preconditions.checkNotNull(context, "No context exists with name: %s", fileName);
        return context;
    }

    private static Map<String, Object> addBqProperties(String datasetName, File tableJsonFile, String extension, StepConfig stepConfig, String tableName) {
        Map<String, Object> properties = stepConfig.getProperties();
        properties.put("sourcePath", tableJsonFile.getAbsolutePath());
        properties.put("dataset", datasetName);
        properties.put("table", tableName);
        properties.put("sourceFormat", extension);
        return properties;
    }

    private static Path getTargetFolderPath(File testCaseFolder, String folderName) {
        Path folderPath = Paths.get(testCaseFolder.getAbsolutePath(), folderName);
        Preconditions.checkArgument(Files.isDirectory(folderPath), "%s folder does not exists %s", folderName, folderPath);
        return folderPath;
    }

    private static String moderateFilePathSlashes(String path) {
        path.replace('\\', File.separatorChar);
        path.replace('/', File.separatorChar);

        return path;
    }

    private static String checkExecutionType(String executionType) {
       try {
           ExecutionType.valueOf(executionType);
        } catch (Exception e) {
           throw new RuntimeException(String.format("Execution type %s dose not exists", executionType));
       }
       return executionType;
    }
}
