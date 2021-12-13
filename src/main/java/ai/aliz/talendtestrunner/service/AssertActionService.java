package ai.aliz.talendtestrunner.service;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.context.TestContextType;
import ai.aliz.jarvis.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.service.assertor.Assertor;
import ai.aliz.talendtestrunner.service.assertor.BqAssertor;
import ai.aliz.talendtestrunner.service.assertor.MySQLAssertor;

@Service
@Slf4j
public class AssertActionService {

    @Autowired
    private ApplicationContext applicationContext;

    private static Map<TestContextType, Class<? extends Assertor>> assertActionTypeMap = new HashMap<>();

    static {
        assertActionTypeMap.put(TestContextType.BigQuery, BqAssertor.class);
        assertActionTypeMap.put(TestContextType.MySQL, MySQLAssertor.class);
    }
    
    public void assertResult(AssertActionConfig assertActionConfig, TestContextLoader contextLoader) {
        log.info("========================================================");
        log.info("Executing assertAction: {}", assertActionConfig);
        
        TestContext context = contextLoader.getContext(assertActionConfig.getSystem());
        Class<? extends Assertor> assertActionClass = null;

        assertActionClass = Objects.requireNonNull(assertActionTypeMap.get(context.getContextType()));

        Assertor assertor = applicationContext.getBean(assertActionClass);
        assertor.doAssert(assertActionConfig);
        
        log.info("Assert action finished");
        log.info("========================================================");
        
    }

}
