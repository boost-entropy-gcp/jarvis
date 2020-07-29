package ai.aliz.talendtestrunner.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.context.Type;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;

@Service
@Slf4j
public class AssertActionService {
    
    @Autowired
    private BigQueryAssertor bigQueryAssertor;
    
    @Autowired
    private TalendJobStateChecker talendJobStateChecker;
    
    public void assertResult(AssertActionConfig assertActionConfig, ContextLoader contextLoader) {
        log.info("========================================================");
        log.info("Executing assertAction: {}", assertActionConfig);
        
        Context context = contextLoader.getContext(assertActionConfig.getSystem());
        
        Type type = context.getType();
        
        switch (type) {
            case BigQuery:
                switch (assertActionConfig.getType()) {
                    case "AssertDataEquals":
                        bigQueryAssertor.assertTable(assertActionConfig, context);
                        break;
                    case "AssertNoChange":
                        bigQueryAssertor.assertNoChange(assertActionConfig, context);
                        break;
                    default:
                        throw new UnsupportedOperationException(String.format("Not supported assert type %s for context type %s", assertActionConfig.getType(), type));
                }
                
                break;
            case MySQL:
                switch (assertActionConfig.getType()) {
                    case "AssertTalendJobState": {
                        String expectedData = TestRunnerUtil.getSourceContentFromConfigProperties(assertActionConfig);
                        talendJobStateChecker.checkJobState(expectedData, context);
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Not supported type: " + type);
        }
        
        log.info("Assert action finished");
        log.info("========================================================");
        
    }
    
}
