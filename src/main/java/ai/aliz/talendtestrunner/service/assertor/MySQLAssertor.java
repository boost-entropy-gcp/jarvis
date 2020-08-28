package ai.aliz.talendtestrunner.service.assertor;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.AssertActionService;
import ai.aliz.talendtestrunner.service.TalendJobStateChecker;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MySQLAssertor implements Assertor {

    @Autowired
    private ContextLoader contextLoader;

    @Autowired
    private AssertActionService assertActionService;

    @Autowired
    private TalendJobStateChecker talendJobStateChecker;

    @Override
    public void doAssert(AssertActionConfig config) {
        assertWithMySQL(config, contextLoader.getContext(config.getSystem()));
    }

    public void assertWithMySQL(AssertActionConfig assertActionConfig, Context context) {
        switch (assertActionConfig.getType()) {
            case "AssertTalendJobState": {
                String expectedData = TestRunnerUtil.getSourceContentFromConfigProperties(assertActionConfig);
                talendJobStateChecker.checkJobState(expectedData, context);
            }
        }
    }
}
