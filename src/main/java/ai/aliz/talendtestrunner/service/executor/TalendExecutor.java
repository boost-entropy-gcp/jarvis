package ai.aliz.talendtestrunner.service.executor;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.TestRunnerService;
import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TalendExecutor implements Executor {

    @Autowired
    private ContextLoader contextLoader;

    @Autowired
    private TestRunnerService testRunnerService;

    @Override
    public void execute(ExecutionActionConfig config) {
        testRunnerService.runTalendJob(contextLoader, config);
    }
}
