package ai.aliz.talendtestrunner;

import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
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
    
    private static final String API_URL = "apiUrl";
    public static String contextPath = null;

    @Autowired
    private TestRunnerService testRunnerService;
    
    @Autowired
    private ContextLoader contextLoader;
    
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    
    @Parameterized.Parameters(name = "{0}")
    @SneakyThrows
    public static Collection jobNames() {
        ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
        InputStream input = IntegrationTestRunner.class.getClassLoader().getResourceAsStream("test.properties");
        Properties properties = new Properties();
        properties.load(input);
        String configPath = properties.getProperty("test.config.path");
        contextPath = properties.getProperty("test.context.path");
        contextLoader.parseContext(contextPath);
        return TestSuite.readTestConfig(configPath, contextLoader)
                        .listTestCases()
                        .stream()
                        .map(testCase1 -> new Object[]{testCase1.getPath().substring(configPath.length()), testCase1})
                        .collect(Collectors.toList());
    }

    private String name;
    private TestCase testCase;
    
    public IntegrationTestRunner(String name, TestCase testCase) {
        this.name = name;
        this.testCase = testCase;
    }
    
    @Test
    public void runTestCase() throws Exception {
        contextLoader.parseContext(contextPath);
        testRunnerService.runTest(testCase);
    }
}
