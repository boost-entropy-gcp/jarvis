package ai.aliz.jarvis.service.executeaction;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import com.google.common.io.Files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.db.BigQueryExecutor;
import ai.aliz.jarvis.testconfig.ExecutionActionConfig;
import ai.aliz.jarvis.testconfig.ExecutionType;
import ai.aliz.talendtestrunner.helper.Helper;

@Component
@Slf4j
public class ExecuteActionService {
    
    @Autowired
    private BigQueryExecutor bigQueryExecutor;
    
    @Autowired
    private TestContextLoader contextLoader;
    
    public void run(List<ExecutionActionConfig> executionActionConfigList) {
        executionActionConfigList.forEach(this::run);
    }
    
    @SneakyThrows
    public void run(ExecutionActionConfig executionActionConfig) {
        log.info("Starting execute action: {}", executionActionConfig);
        
        ExecutionType executionType = executionActionConfig.getType();
        switch (executionType) {
            case NoOps:
                log.info("NoOps execution...");
                break;
            case BqQuery:
                String executionContextId = executionActionConfig.getExecutionContext();
                TestContext executionContext = contextLoader.getContext(executionContextId);
                Objects.requireNonNull(executionContext, "There is no context with id: " + executionContextId);
                String scriptPath = (String) executionActionConfig.getProperties().get(Helper.SOURCE_PATH);
                String script = Files.asCharSource(new File(scriptPath), StandardCharsets.UTF_8).read();
                bigQueryExecutor.executeScript(script, executionContext);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Not supported executionType: " + executionType));
        }
        
        log.info("Execute action finished: {}", executionActionConfig);
    }
}
