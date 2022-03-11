package ai.aliz.talendtestrunner;

import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;


import ai.aliz.jarvis.context.JarvisContextLoader;
import ai.aliz.jarvis.config.JarvisTestCase;
import ai.aliz.talendtestrunner.service.JarvisRunnerService;


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
    private JarvisRunnerService jarvisRunnerService;
    
    @Autowired
    private JarvisContextLoader contextLoader;
    
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    
    @Parameterized.Parameters(name = "{0}")
    @SneakyThrows
    public static Collection jobNames() {
//        ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
        InputStream input = IntegrationTestRunner.class.getClassLoader().getResourceAsStream("test.properties");
        Properties properties = new Properties();
        properties.load(input);
        String configPath = properties.getProperty("test.config.path");
        contextPath = properties.getProperty("test.context.path");
//        contextLoader.parseContext(contextPath);
//        return TestSuite.readTestConfig(configPath, contextLoader)
//                .listTestCases()
//                .stream()
//                .map(testCase1 -> new Object[]{testCase1.getPath().substring(configPath.length()), testCase1})
//                .collect(Collectors.toList());
        return null;
    }

    private String name;
    private JarvisTestCase jarvisTestCase;
    
    public IntegrationTestRunner(String name, JarvisTestCase jarvisTestCase) {
        this.name = name;
        this.jarvisTestCase = jarvisTestCase;
    }
    
    @Test
    public void runJarvisTestCase() throws Exception {
//        contextLoader.parseContext(contextPath);
        jarvisRunnerService.runJarvis(jarvisTestCase);
    }
}
