package ai.aliz.talendtestrunner.service;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.testconfig.InitActionConfig;

import static ai.aliz.talendtestrunner.helper.Helper.SOURCE_PATH;


@Service
@Log4j2
public class SftpService {
    
    public static final String MODIFIED_AT_MARKER_PART = "_MODTIME_";
    
    private Set<TestContext> alreadyUsed = Sets.newHashSet();
    
    public void loadFilesToSftp(InitActionConfig initActionConfig, TestContext sftpContext) {
        ChannelSftp channelSftp = setupJsch(sftpContext);
        String remoteBasePath = sftpContext.getParameter("remoteBasePath");
        cleanup(channelSftp, remoteBasePath);
        
        String localRootFolderPath = (String) initActionConfig.getProperties().get(SOURCE_PATH);
        
        File localRootFolder = new File(localRootFolderPath);
        loadFolder(localRootFolder, remoteBasePath, channelSftp);
        
    }
    
    @SneakyThrows
    private void loadFolder(File localFolder, String remotePath, ChannelSftp channelSftp) {
        
        Preconditions.checkArgument(localFolder.isDirectory(), "%s is not a directory", localFolder);
        
        for (File child : localFolder.listFiles()) {
            String remoteChildPath = remotePath + "/" + child.getName();
            if (child.isFile()) {
                channelSftp.put(child.toString(), remoteChildPath);
            } else {
                loadFolder(child, remoteChildPath, channelSftp);
            }
        }
        
    }
    
    @SneakyThrows
    public void prepareFolder(Path localFile, TestContext sftpContext) {
        try {
            ChannelSftp channelSftp = setupJsch(sftpContext);
            
            String fileAbsolutePath = localFile.toFile().getAbsolutePath();
            
            String fileName = localFile.getFileName().toString();
            
            RemoteFileMeta remoteFileMeta = prepareRemoteFileMeta(fileName);
            String remoteBasePath = sftpContext.getParameter("remoteBasePath");
            String fileRemotePath = remoteBasePath + "/" +
                    remoteFileMeta.getFilename();
            
            synchronized (sftpContext) {
                // this is ugly, but a prettier solution would require a bigger redesign
                if (!alreadyUsed.contains(sftpContext)) {
                    cleanup(channelSftp, remoteBasePath);
                    alreadyUsed.add(sftpContext);
                }
            }
            
            log.info("Putting file {} to remote path {}", fileAbsolutePath, fileRemotePath);
            channelSftp.put(localFile.toString(), fileRemotePath);
            if (remoteFileMeta.getModifiedAtEpochSeconds() != null) {
                channelSftp.setMtime(fileRemotePath, remoteFileMeta.getModifiedAtEpochSeconds());
            }
        } catch (SftpException e) {
            throw new IllegalStateException("Error while setting up SFTP", e);
        }
    }
    
    @SneakyThrows
    private void cleanup(ChannelSftp channelSftp, String remoteBasePath) {
        log.info("Cleaning up: {}", remoteBasePath);
        Collection<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(remoteBasePath);
        for (ChannelSftp.LsEntry item : fileAndFolderList) {
            if (item.getFilename().equals(".") || item.getFilename().equals("..")) {
                continue;
            }
            String itemPath = remoteBasePath + "/" + item.getFilename();
            if (!item.getAttrs().isDir()) {
                channelSftp.rm(itemPath); // Remove file.
            } else {
                cleanup(channelSftp, itemPath);
                channelSftp.rm(itemPath);
            }
        }
    }
    
    private RemoteFileMeta prepareRemoteFileMeta(String fileName) {
        RemoteFileMeta remoteFileMeta = new RemoteFileMeta();
        if (fileName.contains(MODIFIED_AT_MARKER_PART)) {
            String afterModifiedAt = fileName.split(MODIFIED_AT_MARKER_PART)[1];
            String modifiedTimestamp = afterModifiedAt.replace(".csv", "");
            
            LocalDateTime modifiedDateTime = LocalDateTime.parse(modifiedTimestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm[_ss_SSS]"));
            remoteFileMeta.setModifiedAtEpochSeconds((int) modifiedDateTime.toEpochSecond(ZoneOffset.ofHours(0)));
            
            String remoteFilename = fileName.replaceAll(String.format("%s[^.]*", MODIFIED_AT_MARKER_PART), "");
            remoteFileMeta.setFilename(remoteFilename);
        } else {
            remoteFileMeta.setFilename(fileName);
        }
        
        return remoteFileMeta;
    }
    
    @Data
    private static class RemoteFileMeta {
        
        private String filename;
        private Integer modifiedAtEpochSeconds;
    }
    
    @SneakyThrows
    private ChannelSftp setupJsch(TestContext sftpContext) {
        JSch jsch = new JSch();
        Session jschSession = jsch.getSession(sftpContext.getParameter("user"), sftpContext.getParameter("host"));
        
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        jschSession.setConfig(config);
        jschSession.setPassword(sftpContext.getParameter("password"));
        jschSession.setPort(Integer.parseInt(sftpContext.getParameter("port")));
        
        jschSession.connect();
        ChannelSftp channel = (ChannelSftp) jschSession.openChannel("sftp");
        channel.connect();
        return channel;
    }
}
