package ai.aliz.talendtestrunner.service.executor;

import ai.aliz.jarvis.testconfig.ExecutionActionConfig;
import org.springframework.stereotype.Service;

@Service
public class NoOpsExecutor implements Executor {

    @Override
    public void execute(ExecutionActionConfig config) {

    }
}
