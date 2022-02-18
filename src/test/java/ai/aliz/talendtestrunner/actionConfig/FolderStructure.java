package ai.aliz.talendtestrunner.actionConfig;

import lombok.SneakyThrows;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class FolderStructure {
    
    @Test
    @SneakyThrows
    public void testFolderStructure() {
//        TestSuite testSuite = new TestSuite();
//        ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
//        String contextPath = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test_context.json").getFile())).getPath();
//        String configPath = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test_structure").getFile())).getPath();
//        contextLoader.parseContext(contextPath);
//        List<TestCase> testCases = testSuite.readTestConfig(configPath, contextLoader).getTestCases();
//
//        AssertActionConfig assertActionConfig = testCases.get(0).getAssertActionConfigs().get(0);
//        InitActionConfig initActionConfigBq = testCases.get(0).getInitActionConfigs().get(0);
//        InitActionConfig initActionConfigSQL = testCases.get(0).getInitActionConfigs().get(1);
//
//        assertThat(assertActionConfig.getProperties().get("sourcePath"), is(Paths.get(configPath, "test_json", "assert" , "TEST_ID" , "test_dataset" , "assertTest.json").toString()));
//        assertThat(initActionConfigBq.getProperties().get("sourcePath"), is(Paths.get(configPath, "test_json" , "pre" , "TEST_ID" , "test_dataset" , "init.json").toString()));
//        assertThat(initActionConfigSQL.getProperties().get("sourcePath"), is(Paths.get(configPath, "test_json" , "pre" , "TEST_ID.sql").toString()));
    }
}
