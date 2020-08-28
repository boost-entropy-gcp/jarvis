package ai.aliz.talendtestrunner.service.initAction;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.SftpService;
import ai.aliz.talendtestrunner.testconfig.InitActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SFTPLoadInitiator implements Initiator {

    @Autowired
    private ContextLoader contextLoader;

    @Autowired
    private SftpService sftpService;

    @Override
    public void doInitAction(InitActionConfig config) {
        sftpService.loadFilesToSftp(config, contextLoader.getContext(config.getSystem()));
    }
}
