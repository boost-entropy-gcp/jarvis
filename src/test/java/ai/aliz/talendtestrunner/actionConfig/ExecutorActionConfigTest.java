package ai.aliz.talendtestrunner.actionConfig;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.helper.TestHelper;
import ai.aliz.talendtestrunner.service.AssertServiceTest;
import ai.aliz.talendtestrunner.service.ExecutionActionConfigCreator;
import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;
import ai.aliz.talendtestrunner.testconfig.ExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ExecutorActionConfigTest {

    private ExecutionActionConfigCreator executionActionConfigCreator = new ExecutionActionConfigCreator();

    @MockBean
    private BigQuery bigQuery;

    @Test
    public void testExecutorListCreation() {
        ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
        String contextPath = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test_context.json").getFile())).getPath();
        contextLoader.parseContext(contextPath);
        List<Map<String, String>> executions = new ArrayList<>();
        Map<String, String> execution = new HashMap<>();
        execution.put("executionType", "BqQuery");
        execution.put("queryPath", "\\test\\test.json");
        execution.put("executionContext", "TEST");
        executions.add(execution);

        List<ExecutionActionConfig> executionActionConfigs = executionActionConfigCreator.getExecutionActionConfigs(contextLoader, executions);

        ExecutionActionConfig executionActionConfig = executionActionConfigs.get(0);
        assertThat(executionActionConfig.getProperties().get("sourcePath"), is(TestHelper.addSeparator("C:\\test\\project\\test\\test.json")));
        assertThat(executionActionConfig.getType(), is(ExecutionType.BqQuery));
        assertThat(executionActionConfig.getExecutionContext(), is("TEST"));
    }
}
