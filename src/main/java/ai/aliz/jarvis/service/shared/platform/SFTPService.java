package ai.aliz.jarvis.service.shared.platform;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

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

import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.config.InitActionConfig;

import static ai.aliz.jarvis.util.JarvisConstants.HOST;
import static ai.aliz.jarvis.util.JarvisConstants.PASSWORD;
import static ai.aliz.jarvis.util.JarvisConstants.PORT;
import static ai.aliz.jarvis.util.JarvisConstants.REMOTE_BASE_PATH;
import static ai.aliz.jarvis.util.JarvisConstants.SFTP;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_PATH;
import static ai.aliz.jarvis.util.JarvisConstants.USER;

@Service
@Slf4j
public class SFTPService {
    
    private static final String MODIFIED_AT_MARKER_PART = "_MODTIME_";
    
    private Set<JarvisContext> alreadyUsed = Sets.newHashSet();
    
    public void loadFilesToSftp(InitActionConfig initActionConfig, JarvisContext sftpContext) {
        ChannelSftp channelSftp = setupJsch(sftpContext);
        String remoteBasePath = sftpContext.getParameter(REMOTE_BASE_PATH);
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
    public void prepareFolder(Path localFile, JarvisContext sftpContext) {
        try {
            ChannelSftp channelSftp = setupJsch(sftpContext);
            
            String fileAbsolutePath = localFile.toFile().getAbsolutePath();
            
            String fileName = localFile.getFileName().toString();
            
            SFTPService.RemoteFileMeta remoteFileMeta = prepareRemoteFileMeta(fileName);
            String remoteBasePath = sftpContext.getParameter(REMOTE_BASE_PATH);
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
    
    private SFTPService.RemoteFileMeta prepareRemoteFileMeta(String fileName) {
        SFTPService.RemoteFileMeta remoteFileMeta = new SFTPService.RemoteFileMeta();
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
    
    @SneakyThrows
    private ChannelSftp setupJsch(JarvisContext sftpContext) {
        JSch jsch = new JSch();
        Session jschSession = jsch.getSession(sftpContext.getParameter(USER), sftpContext.getParameter(HOST));
        
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        jschSession.setConfig(config);
        jschSession.setPassword(sftpContext.getParameter(PASSWORD));
        jschSession.setPort(Integer.parseInt(sftpContext.getParameter(PORT)));
        
        jschSession.connect();
        ChannelSftp channel = (ChannelSftp) jschSession.openChannel(SFTP);
        channel.connect();
        return channel;
    }
    
    @Data
    private static class RemoteFileMeta {
        private String filename;
        private Integer modifiedAtEpochSeconds;
    }
}