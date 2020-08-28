package ai.aliz.talendtestrunner.actionConfig;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.helper.Helper;
import ai.aliz.talendtestrunner.helper.TestHelper;
import ai.aliz.talendtestrunner.service.AssertServiceTest;
import ai.aliz.talendtestrunner.service.ExecutionActionConfigCreator;
import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class ExecutorActionConfigTest {

    private ExecutionActionConfigCreator executionActionConfigCreator = new ExecutionActionConfigCreator();

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

        Assert.assertThat(executionActionConfigs.get(0).getProperties().get("sourcePath"), is(TestHelper.addSeparator("C:\\test\\project\\test\\test.json")));
        Assert.assertThat(executionActionConfigs.get(0).getType(), is("E"));
    }
}
