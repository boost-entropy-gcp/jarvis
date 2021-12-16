package ai.aliz.jarvis.service.assertaction;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.context.TestContextType;
import ai.aliz.jarvis.testconfig.AssertActionConfig;

@Component
@Slf4j
public class AssertActionService {
    
    @Autowired
    private TestContextLoader contextLoader;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private static Map<TestContextType, Class<? extends Assertor>> assertActionTypeMap = new HashMap<>();
    
    static {
        assertActionTypeMap.put(TestContextType.BigQuery, BqAssertor.class);
        assertActionTypeMap.put(TestContextType.MySQL, MySQLAssertor.class);
    }
    
    public void run(List<AssertActionConfig> assertActionConfigList) {
        assertActionConfigList.forEach(this::run);
    }
    
    public void run(AssertActionConfig assertActionConfig) {
        log.info("Starting assert action: {}", assertActionConfig);
        TestContext context = contextLoader.getContext(assertActionConfig.getSystem());
        Class<? extends Assertor> assertActionClass = null;
        
        assertActionClass = Objects.requireNonNull(assertActionTypeMap.get(context.getContextType()));
        
        Assertor assertor = applicationContext.getBean(assertActionClass);
        assertor.doAssert(assertActionConfig);
        log.info("Assert action finished: {}", assertActionConfig);
    }
}
