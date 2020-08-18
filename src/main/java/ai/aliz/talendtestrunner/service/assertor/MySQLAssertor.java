package ai.aliz.talendtestrunner.service.assertor;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.AssertActionService;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MySQLAssertor implements Assertor {

    @Autowired
    private ContextLoader contextLoader;

    @Autowired
    private AssertActionService assertActionService;

    @Override
    public void doAssert(AssertActionConfig config) {
        assertActionService.assertWithMySQL(config, contextLoader.getContext(config.getSystem()));
    }
}
