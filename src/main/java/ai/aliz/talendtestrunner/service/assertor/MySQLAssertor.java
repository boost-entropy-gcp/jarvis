package ai.aliz.talendtestrunner.service.assertor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.service.AssertActionService;
import ai.aliz.talendtestrunner.service.TalendJobStateChecker;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;

@Service
public class MySQLAssertor implements Assertor {

    @Autowired
    private TestContextLoader contextLoader;

    @Autowired
    private AssertActionService assertActionService;

    @Autowired
    private TalendJobStateChecker talendJobStateChecker;

    @Override
    public void doAssert(AssertActionConfig config) {
        assertWithMySQL(config, contextLoader.getContext(config.getSystem()));
    }

    public void assertWithMySQL(AssertActionConfig assertActionConfig, TestContext context) {
        switch (assertActionConfig.getType()) {
            case "AssertTalendJobState": {
                String expectedData = TestRunnerUtil.getSourceContentFromConfigProperties(assertActionConfig);
                talendJobStateChecker.checkJobState(expectedData, context);
            }
        }
    }
}
