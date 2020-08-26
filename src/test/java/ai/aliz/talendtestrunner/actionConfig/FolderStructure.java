package ai.aliz.talendtestrunner.actionConfig;

import ai.aliz.talendtestrunner.IntegrationTestRunner;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.AssertServiceTest;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.testconfig.InitActionConfig;
import ai.aliz.talendtestrunner.testconfig.TestCase;
import ai.aliz.talendtestrunner.testconfig.TestSuite;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class FolderStructure {

    @Test
    @SneakyThrows
    public void testFolderStructure() {
        TestSuite testSuite = new TestSuite();
        ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
        String contextPath = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test_context.json").getFile())).getPath();
        InputStream input = IntegrationTestRunner.class.getClassLoader().getResourceAsStream("test.properties");
        Properties properties = new Properties();
        properties.load(input);
        String configPath = properties.getProperty("test.structure.config.path");
        contextLoader.parseContext(contextPath);
        List<TestCase> testCases = testSuite.readTestConfig(configPath, contextLoader).getTestCases();

        AssertActionConfig assertActionConfig = testCases.get(0).getAssertActionConfigs().get(0);
        InitActionConfig initActionConfigBq = testCases.get(0).getInitActionConfigs().get(0);
        InitActionConfig initActionConfigSQL = testCases.get(0).getInitActionConfigs().get(1);

        assertThat(assertActionConfig.getProperties().get("sourcePath"), is(configPath + "\\test_json\\assert\\TEST_ID\\test_dataset\\assertTest.json"));
        assertThat(initActionConfigBq.getProperties().get("sourcePath"), is(configPath + "\\test_json\\pre\\TEST_ID\\test_dataset\\init.json"));
        assertThat(initActionConfigSQL.getProperties().get("sourcePath"), is(configPath + "\\test_json\\pre\\TEST_ID.sql"));
    }
}
