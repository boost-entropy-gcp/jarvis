package ai.aliz.talendtestrunner.actionConfig;

import com.google.cloud.bigquery.BigQuery;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Ignore
public class ExecutorActionConfigTest {

//    private ExecutionActionConfigCreator executionActionConfigCreator = new ExecutionActionConfigCreator();

    @MockBean
    private BigQuery bigQuery;

    @Test
    public void testExecutorListCreation() {
//        ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
//        String contextPath = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test_context.json").getFile())).getPath();
//        contextLoader.parseContext(contextPath);
//        List<Map<String, String>> executions = new ArrayList<>();
//        Map<String, String> execution = new HashMap<>();
//        execution.put("executionType", "BqQuery");
//        execution.put("queryPath", "\\test\\test.json");
//        execution.put("executionContext", "TEST");
//        executions.add(execution);
//
//        List<ExecutionActionConfig> executionActionConfigs = executionActionConfigCreator.getExecutionActionConfigs(contextLoader, executions);
//
//        ExecutionActionConfig executionActionConfig = executionActionConfigs.get(0);
//        assertThat(executionActionConfig.getProperties().get("sourcePath"), is("C:\\test\\project\\test\\test.json"));
//        assertThat(executionActionConfig.getType(), is(ExecutionType.BqQuery));
//        assertThat(executionActionConfig.getExecutionContext(), is("TEST"));
    }
}
