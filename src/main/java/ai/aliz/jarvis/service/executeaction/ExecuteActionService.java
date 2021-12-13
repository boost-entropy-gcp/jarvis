package ai.aliz.jarvis.service.executeaction;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.aliz.jarvis.testconfig.ExecutionActionConfig;

@Component
@Slf4j
public class ExecuteActionService {
    
    public void run(List<ExecutionActionConfig> executionActionConfigList) {
        executionActionConfigList.forEach(this::run);
    }
    
    public void run(ExecutionActionConfig executionActionConfig) {
        log.info("Starting execute action: {}", executionActionConfig);
        log.info("Execute action finished: {}", executionActionConfig);
    }
}
