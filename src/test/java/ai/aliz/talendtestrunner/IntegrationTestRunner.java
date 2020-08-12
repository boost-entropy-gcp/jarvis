package ai.aliz.talendtestrunner;

import lombok.SneakyThrows;

import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.TestRunnerService;
import ai.aliz.talendtestrunner.testconfig.TestCase;
import ai.aliz.talendtestrunner.testconfig.TestSuite;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@SpringBootTest
public class IntegrationTestRunner {
    
    public static final String CONTEXT_PATH = getPathFromProperties("test.context.path");
    
    @Autowired
    private TestRunnerService testRunnerService;
    
    @Autowired
    private ContextLoader contextLoader;
    
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    
    @Parameterized.Parameters(name = "{0}")
    public static Collection jobNames() {
        String configPath = getPathFromProperties("test.config.path");
        ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
        contextLoader.parseContext(CONTEXT_PATH);
        return TestSuite.readTestConfig(configPath, contextLoader)
                        .listTestCases()
                        .stream()
                        .map(testCase1 -> new Object[]{testCase1.getPath().substring(configPath.length()), testCase1})
                        .collect(Collectors.toList());
    }

    @SneakyThrows
    private static String getPathFromProperties(String key) {
        Properties properties = new Properties();
        properties.load(IntegrationTestRunner.class.getClassLoader().getResourceAsStream("test.properties"));
        return properties.getProperty(key);
    }

    private String name;
    private TestCase testCase;
    
    public IntegrationTestRunner(String name, TestCase testCase) {
        this.name = name;
        this.testCase = testCase;
    }
    
    @Test
    public void runTestCase() throws Exception {
        contextLoader.parseContext(CONTEXT_PATH);
        testRunnerService.runTest(testCase);
    }
}
