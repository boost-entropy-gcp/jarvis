package ai.aliz.talendtestrunner.service.executor;

import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import ai.aliz.talendtestrunner.service.executor.Executor;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;

@Service
public class BqScriptExecutor implements Executor {
    
    @Autowired
    private ContextLoader contextLoader;

    @Autowired
    private BigQueryExecutor bigQueryExecutor;
    
    @Override
    public void execute(ExecutionActionConfig config) {
        bigQueryExecutor.executeQuery(TestRunnerUtil.getSourceContentFromConfigProperties(config), contextLoader.getContext(config.getExecutionContext()));
    }
}
