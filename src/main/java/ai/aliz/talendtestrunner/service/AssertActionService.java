package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.service.assertor.Assertor;
import ai.aliz.talendtestrunner.service.assertor.BqAssertor;
import ai.aliz.talendtestrunner.service.assertor.MySQLAssertor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.context.ContextType;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class AssertActionService {

    @Autowired
    private ApplicationContext applicationContext;

    private static Map<ContextType, Class<? extends Assertor>> assertActionTypeMap = new HashMap<>();

    static {
        assertActionTypeMap.put(ContextType.BigQuery, BqAssertor.class);
        assertActionTypeMap.put(ContextType.MySQL, MySQLAssertor.class);
    }
    
    public void assertResult(AssertActionConfig assertActionConfig, ContextLoader contextLoader) {
        log.info("========================================================");
        log.info("Executing assertAction: {}", assertActionConfig);
        
        Context context = contextLoader.getContext(assertActionConfig.getSystem());
        Class<? extends Assertor> assertActionClass = null;

        assertActionClass = Objects.requireNonNull(assertActionTypeMap.get(context.getContextType()));

        Assertor assertor = applicationContext.getBean(assertActionClass);
        assertor.doAssert(assertActionConfig);
        
        log.info("Assert action finished");
        log.info("========================================================");
        
    }

}
