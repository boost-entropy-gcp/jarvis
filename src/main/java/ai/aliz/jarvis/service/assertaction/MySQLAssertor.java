package ai.aliz.jarvis.service.assertaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.JarvisContextLoader;
import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.config.AssertActionConfig;

@Service
public class MySQLAssertor implements Assertor {

    @Autowired
    private JarvisContextLoader contextLoader;

    @Autowired
    private AssertActionService assertActionService;

//    @Autowired
//    private TalendJobStateChecker talendJobStateChecker;

    @Override
    public void doAssert(AssertActionConfig config) {
        assertWithMySQL(config, contextLoader.getContext(config.getSystem()));
    }

    public void assertWithMySQL(AssertActionConfig assertActionConfig, JarvisContext context) {
        throw new UnsupportedOperationException("Not supported yet"); //FIXME
//        switch (assertActionConfig.getType()) {
//            case "AssertTalendJobState": {
//                String expectedData = TestRunnerUtil.getSourceContentFromConfigProperties(assertActionConfig);
//                talendJobStateChecker.checkJobState(expectedData, context);
//            }
//        }
    }
}
