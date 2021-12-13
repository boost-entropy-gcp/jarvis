package ai.aliz.talendtestrunner.service.executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.testconfig.ExecutionActionConfig;
import ai.aliz.talendtestrunner.service.TestRunnerService;

@Service
public class TalendExecutor implements Executor {

    @Autowired
    private TestContextLoader contextLoader;

    @Autowired
    private TestRunnerService testRunnerService;

    @Override
    public void execute(ExecutionActionConfig config) {
        testRunnerService.runTalendJob(contextLoader, config);
    }
}
