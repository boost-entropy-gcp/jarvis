package ai.aliz.jarvis.service.initaction.initiator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.service.shared.platform.SFTPService;
import ai.aliz.jarvis.testconfig.InitActionConfig;

@Service
public class SFTPLoadInitiator implements Initiator {
    
    @Autowired
    private TestContextLoader contextLoader;
    
    @Autowired
    private SFTPService sftpService;
    
    @Override
    public void doInitAction(InitActionConfig config) {
        sftpService.loadFilesToSftp(config, contextLoader.getContext(config.getSystem()));
    }
}
