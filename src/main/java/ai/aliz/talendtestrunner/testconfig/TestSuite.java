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
import java.util.Arrays;
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
import ai.aliz.talendtestrunner.context.Type;

@Data
@ToString(exclude = "parentSuite")
public class TestSuite {
    
    public static final String TEST_SUITE_FILE_NAME = "testSuite.json";
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
            
            testSuite.setProperties((Map<String, Object>) testSuiteMap.get("properties"));
            
            Boolean caseAutoDetect = (Boolean) testSuiteMap.get("caseAutoDetect");
            testSuite.setCaseAutoDetect(caseAutoDetect);
            
            Map<String, Object> defaultProperties = (Map<String, Object>) testSuiteMap.getOrDefault("defaultProperties", new HashMap<>());
            List<Map<String, String>> executionActions = (List<Map<String, String>>)testSuiteMap.getOrDefault("executions", Collections.singletonMap("type", "noOps"));

            List<ExecutionActionConfig> executionActionConfigs = getExecutionActionConfigs(contextLoader, executionActions);
            testCase.getExecutionActionConfigs().addAll(executionActionConfigs);

            if (Boolean.TRUE.equals(caseAutoDetect)) {
                List<TestCase> testCases = Files.list(Paths.get(descriptorFolder)).filter(Files::isDirectory).map(path -> {
                    File testCaseFolder = path.toFile();
                    testCase.setPath(testCaseFolder.getAbsolutePath());
                    testCase.setName(testCaseFolder.getName());


                    List<InitActionConfig> initActions = getInitActionConfigs(contextLoader, defaultProperties, testCaseFolder);
                    testCase.getInitActionConfigs().addAll(initActions);

                    List<AssertActionConfig> assertActionConfigs = getAssertActionConfigs(contextLoader, descriptorFolder, defaultProperties, testCase, testCaseFolder);
                    testCase.getAssertActionConfigs().addAll(assertActionConfigs);
                    
                    return testCase;
                }).collect(Collectors.toList());
                
                testSuite.getTestCases().addAll(testCases);
                
            }
            
            List<Map<String, Object>> testCases = (List<Map<String, Object>>) testSuiteMap.get("testCases");
            if (testCases != null) {
                setTestCases(testSuite, descriptorFolder, testCases);
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return testSuite;
        
    }

    private static List<ExecutionActionConfig> getExecutionActionConfigs(ContextLoader contextLoader, List<Map<String, String>> executions) {
//        Path executeFolder = Paths.get(testCaseFolder.getAbsolutePath(), "execution");
//        Preconditions.checkArgument(Files.isDirectory(executeFolder), "Execution folder does not exists %s", executeFolder);
//        List<ExecutionActionConfig> executionAction = null;
        List<ExecutionActionConfig> executionActionConfigs = Lists.newArrayList();

//        try {
//            executionAction = Files.list(executeFolder).flatMap(executionFile -> {
//
//
//                        String executionFileName = executionFile.toFile().getName();
//                        if (Files.isRegularFile(executionFile)) {
//                            ExecutionActionConfig executionActionConfig = new ExecutionActionConfig();
//                            String executionBaseName = FilenameUtils.getBaseName(executionFileName);
//                            Preconditions.checkNotNull(contextLoader.getContext(executionBaseName), "No context exists with name: %s", executionBaseName);
//
//                            executionActionConfig.setSystem(executionBaseName);
//
//                            String executionExtension = FilenameUtils.getExtension(executionFileName);
//
//                            executionActionConfig.setDescriptorFolder(null);
//                            switch (executionExtension) {
//                                case "sql":
//                                    executionActionConfig.setType("BigQuery");
//                                    executionActionConfig.getProperties().put("sourcePath", executionFile.toFile().getAbsolutePath());
//                                    break;
//                                default:
//                                    throw new UnsupportedOperationException("Not supported extension for init action autodetect: " + executionFile);
//
//                            }
//                            executionActionConfigs.add(executionActionConfig);
//                        } else {
//                            ExecutionActionConfig executionActionConfig = new ExecutionActionConfig();
//                            executionActionConfig.setType("noOps");
//                        }
//                return executionActionConfigs.stream();
//            }).collect(Collectors.toList());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        String repositoryRoot = moderateFilePathSlashes(contextLoader.getContext("local").getParameter("repositoryRoot"));
        return executions.stream()
                .flatMap(e -> {
                    ExecutionActionConfig executionActionConfig = new ExecutionActionConfig();
                    executionActionConfig.setType(ExecutionType.valueOf(checkExecutionType(e.get("type"))));
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

//            for (Map<String, Object> executeActionMap : (List<Map<String, Object>>) testCaseMap.get("executeActions")) {
//                final String type = (String) executeActionMap.get("type");
//                switch (type) {
//                    case "TalendTask":
//                        TalendTask talendTask = new TalendTask();
//                        talendTask.setTaskName((String) executeActionMap.get("taskName"));
//                        testCase.getExecuteActions().add(talendTask);
//                        break;
//                    default:
//                        throw new RuntimeException("Unsupported initAction: " + type);
//                }
//            }

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

    private static List<AssertActionConfig> getAssertActionConfigs(ContextLoader contextLoader, String descriptorFolder, Map<String, Object> defaultProperties, TestCase testCase, File testCaseFolder) {
        Path assertFolder = Paths.get(testCaseFolder.getAbsolutePath(), "assert");
        Preconditions.checkArgument(Files.isDirectory(assertFolder), "Assert folder does not exists %s", assertFolder);
        List<AssertActionConfig> assertActionConfigs = null;
        try {
            assertActionConfigs = Files.list(assertFolder).flatMap(assertActionConfigPath -> {
                List<AssertActionConfig> assertActionConfigsForFolder = new ArrayList<>();
                if (Files.isDirectory(assertActionConfigPath)) {

                    File assertContextFolder = assertActionConfigPath.toFile();
                    String directoryName = assertContextFolder.getName();
                    Context context = contextLoader.getContext(directoryName);
                    Preconditions.checkNotNull(context, "There is context for assert folder: " + assertActionConfigPath);
                    String system = context.getId();

                    switch (context.getType()) {
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
                                    Map<String, Object> properties = assertActionConfig.getProperties();
                                    properties.put("dataset", datasetName);
                                    properties.put("table", tableName);

                                    properties.put("assertKeyColumns",
                                                   defaultProperties.getOrDefault("assert.assertKeyColumns", Lists.newArrayList(tableName + "_BID", tableName + "_VALID_FROM")));
                                    properties.put("excludePreviouslyInsertedRows", defaultProperties.getOrDefault("assert.excludePreviouslyInsertedRows", false));
                                    properties.put("sourceFormat", "json");
                                    properties.put("sourcePath", tableDataFile.getAbsolutePath());
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
                                Map<String, Object> properties = assertActionConfig.getProperties();
                                properties.put("sourcePath", tableDataFile.getAbsolutePath());
                                assertActionConfigsForFolder.add(assertActionConfig);

                            }
                            break;

                        default:
                            throw new UnsupportedOperationException("Not supported context type for assert " + context.getType());

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
        Path preFolder = Paths.get(testCaseFolder.getAbsolutePath(), "pre");
        Preconditions.checkArgument(Files.isDirectory(preFolder), "Pre folder does not exists %s", preFolder);
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
                    Context context = contextLoader.getContext(fileName);
                    Preconditions.checkNotNull(context, "No context exists with name: %s", fileName);
                    String system = fileName;

                    Type contextType = context.getType();
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
                                    Map<String, Object> properties = bqLoadInitActionConfig.getProperties();
                                    properties.put("sourcePath", tableJsonFile.getAbsolutePath());
                                    properties.put("dataset", datasetName);
                                    properties.put("table", tableName);
                                    properties.put("sourceFormat", extension);
                                    properties.put("noMetadatAddition", defaultProperties.getOrDefault("init." + context.getId() + ".noMetadatAddition", false));

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
