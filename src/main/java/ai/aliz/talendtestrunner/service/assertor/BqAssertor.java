package ai.aliz.talendtestrunner.service.assertor;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.AssertActionService;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BqAssertor implements Assertor {

    @Autowired
    ContextLoader contextLoader;

    @Autowired
    AssertActionService assertActionService;

    @Override
    public void doAssert(AssertActionConfig config) {
        assertActionService.assertWithBigQuery(config, contextLoader.getContext(config.getSystem()));
    }
}
