package ai.aliz.jarvis.config;

import lombok.SneakyThrows;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ai.aliz.jarvis.context.JarvisContextLoader;
import ai.aliz.jarvis.context.JarvisContext;

@Component
public class ConfigLoader {
    
    public static final String JARVIS_TEST_SUITE_FILE_NAME = "jarvisTestSuite.json";
    public static final String DEFAULT_PROPERTIES_KEY = "defaultProperties";
    public static final String PROPERTIES_KEY = "properties";
    public static final String CASE_AUTO_DETECT_KEY = "caseAutoDetect";
    public static final String JARVIS_TEST_CASES_KEY = "jarvisTestCases";
    public static final String SYSTEM = "system";
    public static final String TYPE = "type";
    
    public final JarvisTestSuite jarvisTestSuite;
    public final Map<String, JarvisContext> contextMap;
    private final InitActionConfigFactory initActionConfigFactory;
    private final ExecutionActionConfigFactory executionActionConfigFactory;
    private final AssertActionConfigFactory assertActionConfigFactory;
    
    @Autowired
    public ConfigLoader(JarvisContextLoader contextLoader,
                        Environment environment,
                        InitActionConfigFactory initActionConfigFactory,
                        ExecutionActionConfigFactory executionActionConfigFactory,
                        AssertActionConfigFactory assertActionConfigFactory) {
        this.contextMap = contextLoader.getContextIdToContexts();
        this.initActionConfigFactory = initActionConfigFactory;
        this.executionActionConfigFactory = executionActionConfigFactory;
        this.assertActionConfigFactory = assertActionConfigFactory;
        this.jarvisTestSuite = this.readJarvisConfig(environment.getProperty("config"));
        
    }
    
    public JarvisTestSuite getJarvisTestSuite() {
        return jarvisTestSuite;
    }
    
    @SneakyThrows
    public JarvisTestSuite readJarvisConfig(String jarvisTestSuitePath) {
        
        File jarvisConfigFile = new File(jarvisTestSuitePath);
        Preconditions.checkArgument(jarvisConfigFile.isDirectory(), "JarvisConfig folder %s is not a directory", jarvisTestSuitePath);
        JarvisTestSuite parentSuite;
        Path jarvisTestSuiteConfigFilePath = Paths.get(jarvisTestSuitePath, JARVIS_TEST_SUITE_FILE_NAME);
        if (jarvisTestSuiteConfigFilePath.toFile().exists()) {
            parentSuite = parseFromJson(jarvisTestSuiteConfigFilePath.toFile(), null);
        } else {
            parentSuite = new JarvisTestSuite();
            String descriptorFolder = jarvisTestSuitePath.endsWith(File.separator) ? jarvisTestSuitePath : jarvisTestSuitePath + File.separator;
            parentSuite.setRootFolder(descriptorFolder);
        }
        Path descriptorFolder = jarvisConfigFile.toPath();
        
        Files.list(descriptorFolder).filter(path -> Files.isDirectory(path)).forEach(path -> processFolder(path, parentSuite));
        
        return parentSuite;
    }
    
    @SneakyThrows
    private void processFolder(Path folder, final JarvisTestSuite parentSuite) {
        Preconditions.checkArgument(Files.isDirectory(folder));
        
        JarvisTestSuite currentSuite = parentSuite;
        File descriptorFile = Paths.get(folder.toString(), JARVIS_TEST_SUITE_FILE_NAME).toFile();
        if (descriptorFile.exists()) {
            currentSuite = parseFromJson(descriptorFile, parentSuite);
        }
        
        final JarvisTestSuite newParent = currentSuite;
        if (currentSuite.getJarvisTestCases().isEmpty()) {
            Files.list(folder).filter(path -> Files.isDirectory(path)).forEach(path -> processFolder(path, newParent));
        }
        
        return;
    }
    
    public JarvisTestSuite parseFromJson(File jarvisConfigFile, JarvisTestSuite parentSuite) {
        
        Gson gson = new Gson();
        JarvisTestSuite jarvisTestSuite = new JarvisTestSuite();
        jarvisTestSuite.setParentSuite(parentSuite);
        if (parentSuite != null) {
            parentSuite.getSuites().add(jarvisTestSuite);
        }
        jarvisTestSuite.setConfigPath(jarvisConfigFile.getAbsolutePath());
        String descriptorFolder = jarvisConfigFile.getParentFile().getAbsolutePath() + File.separator;
        
        jarvisTestSuite.setRootFolder(descriptorFolder);
        try (FileReader fileReader = new FileReader(jarvisConfigFile)) {
            Map jarvisTestSuiteMap = gson.fromJson(fileReader, Map.class);
            
            jarvisTestSuite.setProperties((Map<String, Object>) jarvisTestSuiteMap.get(PROPERTIES_KEY));
            
            Boolean caseAutoDetect = (Boolean) jarvisTestSuiteMap.get(CASE_AUTO_DETECT_KEY);
            jarvisTestSuite.setCaseAutoDetect(caseAutoDetect);
            
            Map<String, Object> defaultProperties = (Map<String, Object>) jarvisTestSuiteMap.getOrDefault(DEFAULT_PROPERTIES_KEY, new HashMap<>());
            
            if (Boolean.TRUE.equals(caseAutoDetect)) {
                List<JarvisTestCase> jarvisTestCases = Files.list(Paths.get(descriptorFolder)).filter(Files::isDirectory).map(path -> {
                    File jarvisTestCaseFolder = path.toFile();
                    JarvisTestCase jarvisTestCase = new JarvisTestCase();
                    jarvisTestCase.setPath(jarvisTestCaseFolder.getAbsolutePath());
                    jarvisTestCase.setName(jarvisTestCaseFolder.getName());
                    
                    List<InitActionConfig> initActions = initActionConfigFactory.getInitActionConfigs(defaultProperties, jarvisTestCaseFolder);
                    jarvisTestCase.getInitActionConfigs().addAll(initActions);
                    
                    List<ExecutionActionConfig> executionActionConfigList = executionActionConfigFactory.getExecutionActionConfig(jarvisTestSuiteMap);
                    jarvisTestCase.getExecutionActionConfigs().addAll(executionActionConfigList); //TODO this has to work without autodetect also
                    
                    List<AssertActionConfig> assertActionConfigs = assertActionConfigFactory.getAssertActionConfigs(defaultProperties, jarvisTestCaseFolder);
                    jarvisTestCase.getAssertActionConfigs().addAll(assertActionConfigs);
                    
                    return jarvisTestCase;
                }).collect(Collectors.toList());
                
                jarvisTestSuite.getJarvisTestCases().addAll(jarvisTestCases);
            }
            
            List<Map<String, Object>> jarvisTestCases = (List<Map<String, Object>>) jarvisTestSuiteMap.get(JARVIS_TEST_CASES_KEY);
            if (jarvisTestCases != null) {
                setJarvisTestCases(jarvisTestSuite, descriptorFolder, jarvisTestCases);
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return jarvisTestSuite;
        
    }
    
    private static void setJarvisTestCases(JarvisTestSuite jarvisTestSuite, String descriptorFolder, List<Map<String, Object>> jarvisTestCases) {
        for (Map<String, Object> jarvisTestCaseMap : jarvisTestCases) {
            JarvisTestCase jarvisTestCase = new JarvisTestCase();
            String caseName = (String) jarvisTestCaseMap.get("name");
            jarvisTestCase.setName(caseName);
            
            List<Map<String, Object>> initActions = (List<Map<String, Object>>) jarvisTestCaseMap.get("initActions");
            for (Map<String, Object> initActionMap : initActions) {
                InitActionConfig initActionConfig = new InitActionConfig();
                initActionConfig.setSystem((String) initActionMap.remove(SYSTEM));
                initActionConfig.setType(InitActionType.valueOf((String) initActionMap.remove(TYPE)));
                initActionConfig.setDescriptorFolder(descriptorFolder + caseName + File.separator);
                initActionConfig.getProperties().putAll(initActionMap);
                
                jarvisTestCase.getInitActionConfigs().add(initActionConfig);
            }
            
            List<Map<String, Object>> assertActions = (List<Map<String, Object>>) jarvisTestCaseMap.get("assertActions");
            for (Map<String, Object> assertActionMap : assertActions) {
                final String type = (String) assertActionMap.get(TYPE);
                
                AssertActionConfig assertActionConfig = new AssertActionConfig();
                assertActionConfig.setSystem((String) assertActionMap.remove(SYSTEM));
                assertActionConfig.setType(type);
                assertActionConfig.setDescriptorFolder(descriptorFolder + caseName + File.separator);
                assertActionConfig.setProperties(assertActionMap);
                jarvisTestCase.getAssertActionConfigs().add(assertActionConfig);
            }
            
            jarvisTestSuite.getJarvisTestCases().add(jarvisTestCase);
        }
    }
}
