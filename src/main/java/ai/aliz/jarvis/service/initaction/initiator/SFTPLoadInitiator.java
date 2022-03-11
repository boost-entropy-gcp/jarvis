package ai.aliz.jarvis.service.initaction.initiator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.JarvisContextLoader;
import ai.aliz.jarvis.service.shared.platform.SFTPService;
import ai.aliz.jarvis.config.InitActionConfig;

@Service
public class SFTPLoadInitiator implements Initiator {
    
    @Autowired
    private JarvisContextLoader contextLoader;
    
    @Autowired
    private SFTPService sftpService;
    
    @Override
    public void doInitAction(InitActionConfig config) {
        sftpService.loadFilesToSftp(config, contextLoader.getContext(config.getSystem()));
    }
}
