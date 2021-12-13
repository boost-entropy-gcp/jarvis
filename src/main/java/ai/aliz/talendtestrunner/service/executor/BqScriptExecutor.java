package ai.aliz.talendtestrunner.service.executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.testconfig.ExecutionActionConfig;
import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;

@Service
public class BqScriptExecutor implements Executor {
    
    @Autowired
    private TestContextLoader contextLoader;

    @Autowired
    private BigQueryExecutor bigQueryExecutor;
    
    @Override
    public void execute(ExecutionActionConfig config) {
        bigQueryExecutor.executeQuery(TestRunnerUtil.getSourceContentFromConfigProperties(config), contextLoader.getContext(config.getExecutionContext()));
    }
}
