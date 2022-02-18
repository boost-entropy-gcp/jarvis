package ai.aliz.jarvis.service.assertaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.testconfig.AssertActionConfig;

@Service
public class MySQLAssertor implements Assertor {

    @Autowired
    private TestContextLoader contextLoader;

    @Autowired
    private AssertActionService assertActionService;

//    @Autowired
//    private TalendJobStateChecker talendJobStateChecker;

    @Override
    public void doAssert(AssertActionConfig config) {
        assertWithMySQL(config, contextLoader.getContext(config.getSystem()));
    }

    public void assertWithMySQL(AssertActionConfig assertActionConfig, TestContext context) {
        throw new UnsupportedOperationException("Not supported yet"); //FIXME
//        switch (assertActionConfig.getType()) {
//            case "AssertTalendJobState": {
//                String expectedData = TestRunnerUtil.getSourceContentFromConfigProperties(assertActionConfig);
//                talendJobStateChecker.checkJobState(expectedData, context);
//            }
//        }
    }
}
