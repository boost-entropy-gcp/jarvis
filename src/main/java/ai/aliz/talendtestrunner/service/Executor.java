package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;

public interface Executor {
    
    public void execute(ExecutionActionConfig config);
}
