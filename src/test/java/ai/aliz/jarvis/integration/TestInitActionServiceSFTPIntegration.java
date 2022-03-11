package ai.aliz.jarvis.integration;

import lombok.SneakyThrows;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.base.Preconditions;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.context.JarvisContextLoader;
import ai.aliz.jarvis.config.InitActionConfigFactory;
import ai.aliz.jarvis.service.initaction.InitActionService;
import ai.aliz.jarvis.config.InitActionConfig;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static ai.aliz.jarvis.util.JarvisConstants.HOST;
import static ai.aliz.jarvis.util.JarvisConstants.PORT;
import static ai.aliz.jarvis.util.JarvisConstants.SFTP;
import static ai.aliz.jarvis.util.JarvisConstants.USER;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = "context=src/test/resources/integration/sftp-context.json")
public class TestInitActionServiceSFTPIntegration {
    
     /*
    PREREQUISITES
     * The test requires an existing GCP project with Compute Engine API enabled.
     * To provide resources for this test, apply the Terraform configurations in the integration folder.
    */
    
    @Autowired
    private InitActionConfigFactory initActionConfigFactory;
    
    @Autowired
    private InitActionService actionService;
    
    @Autowired
    private JarvisContextLoader contextLoader;
    
    @Test
    public void testFileUpload() {
//        ContextLoader contextLoader = new ContextLoader("src/test/resources/integration/sftp-context.json");
        JarvisContext sftpContext = contextLoader.getContext("SFTP");
        ChannelSftp channel = createChannel(sftpContext);
        Preconditions.checkArgument(findTestFileInCurrentFolder(channel).isEmpty());
    
        List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/integration/sftp"));
        System.out.println(actionConfigs.get(0));
        actionService.run(actionConfigs);
    
        ArrayList<String> fileFound = findTestFileInCurrentFolder(channel);
        Preconditions.checkArgument(!fileFound.isEmpty());
    }
    
    @SneakyThrows
    private ChannelSftp createChannel(JarvisContext sftpContext) {
        JSch jsch = new JSch();
        //TODO replace with password based connection
        jsch.addIdentity("~/.ssh/id_rsa");
        Session jschSession = jsch.getSession(sftpContext.getParameter(USER), sftpContext.getParameter(HOST));
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        jschSession.setConfig(config);
        jschSession.setPort(Integer.parseInt(sftpContext.getParameter(PORT)));
        jschSession.connect();
        ChannelSftp channel = (ChannelSftp) jschSession.openChannel(SFTP);
        channel.connect();
        
        Preconditions.checkArgument(channel.isConnected());
        return channel;
    }
    
    @SneakyThrows
    private ArrayList<String> findTestFileInCurrentFolder(ChannelSftp channel) {
        final ArrayList<String> fileFound = new ArrayList<String>();
        ChannelSftp.LsEntrySelector selector = entry -> {
            System.out.println(entry.getFilename());
            if (entry.getFilename().contains("SFTP.txt")) {
                fileFound.add(entry.getFilename());
            }
            return ChannelSftp.LsEntrySelector.CONTINUE;
        };
        channel.ls(channel.pwd(), selector);
        return fileFound;
    }
    
}
