package ai.aliz.talendtestrunner.service.initAction;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.InitActionService;
import ai.aliz.talendtestrunner.testconfig.InitActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BQLoad implements InitAction {

    @Autowired
    private ContextLoader contextLoader;

    @Autowired
    private InitActionService initActionService;

    @Override
    public void doInitAction(InitActionConfig config) {
        initActionService.doBigQueryInitAction(config, contextLoader.getContext(config.getSystem()));
    }
}
