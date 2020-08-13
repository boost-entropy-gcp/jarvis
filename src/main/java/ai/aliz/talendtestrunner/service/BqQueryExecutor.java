package ai.aliz.talendtestrunner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;

@Service
public class BqQueryExecutor implements Executor {
    
    @Autowired
    ContextLoader contextLoader;
    
    @Override
    public void execute(ExecutionActionConfig config) {
        //TODO put here the exection code
    }
}
