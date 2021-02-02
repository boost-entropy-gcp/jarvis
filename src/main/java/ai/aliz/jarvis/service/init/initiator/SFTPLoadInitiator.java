package ai.aliz.jarvis.service.init.initiator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.ContextLoader;
import ai.aliz.jarvis.service.shared.platform.SFTPService;
import ai.aliz.jarvis.testconfig.InitActionConfig;

@Service
public class SFTPLoadInitiator implements Initiator {
    
    @Autowired
    private ContextLoader contextLoader;
    
    @Autowired
    private SFTPService sftpService;
    
    @Override
    public void doInitAction(InitActionConfig config) {
        sftpService.loadFilesToSftp(config, contextLoader.getContext(config.getSystem()));
    }
}
