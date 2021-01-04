package ai.aliz.talendtestrunner.service.executor;

import ai.aliz.talendtestrunner.service.executor.Executor;
import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;
import org.springframework.stereotype.Service;

@Service
public class AirflowExecutor implements Executor {

    @Override
    public void execute(ExecutionActionConfig config) {
        throw new UnsupportedOperationException("Airflow is not implemented.");
    }
}
