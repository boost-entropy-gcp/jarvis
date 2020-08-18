package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.service.assertor.Assertor;
import ai.aliz.talendtestrunner.service.assertor.BqAssertor;
import ai.aliz.talendtestrunner.service.assertor.MySQLAssertor;
import ai.aliz.talendtestrunner.service.initAction.BQLoad;
import ai.aliz.talendtestrunner.service.initAction.InitAction;
import ai.aliz.talendtestrunner.service.initAction.SFTPLoad;
import ai.aliz.talendtestrunner.service.initAction.SQLExec;
import ai.aliz.talendtestrunner.testconfig.InitActionType;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.context.ContextType;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AssertActionService {
    
    @Autowired
    private BigQueryAssertor bigQueryAssertor;
    
    @Autowired
    private TalendJobStateChecker talendJobStateChecker;

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

        try {
            assertActionClass = assertActionTypeMap.get(context.getContextType());
        } catch (Exception e) {
            throw new UnsupportedOperationException("Not supported type: " + context.getContextType());
        }

        Assertor assertor = applicationContext.getBean(assertActionClass);
        assertor.doAssert(assertActionConfig);
        
        log.info("Assert action finished");
        log.info("========================================================");
        
    }

    public void assertWithMySQL(AssertActionConfig assertActionConfig, Context context) {
        switch (assertActionConfig.getType()) {
            case "AssertTalendJobState": {
                String expectedData = TestRunnerUtil.getSourceContentFromConfigProperties(assertActionConfig);
                talendJobStateChecker.checkJobState(expectedData, context);
            }
        }
    }

    public void assertWithBigQuery(AssertActionConfig assertActionConfig, Context context) {
        switch (assertActionConfig.getType()) {
            case "AssertDataEquals":
                bigQueryAssertor.assertTable(assertActionConfig, context);
                break;
            case "AssertNoChange":
                bigQueryAssertor.assertNoChange(assertActionConfig, context);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Not supported assert type %s for context type %s", assertActionConfig.getType(), context.getContextType()));
        }
    }

}
