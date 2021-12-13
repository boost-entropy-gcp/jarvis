package ai.aliz.jarvis.service.assertaction;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.aliz.jarvis.testconfig.AssertActionConfig;

@Component
@Slf4j
public class AssertActionService {
    
    public void run(List<AssertActionConfig> assertActionConfigList) {
        assertActionConfigList.forEach(this::run);
    }
    
    public void run(AssertActionConfig assertActionConfig) {
        log.info("Starting assert action: {}", assertActionConfig);
        log.info("Assert action finished: {}", assertActionConfig);
    }
}
