package ai.aliz.jarvis.service.init;

import lombok.Lombok;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.Context;
import ai.aliz.jarvis.context.ContextLoader;
import ai.aliz.jarvis.context.ContextType;
import ai.aliz.jarvis.service.shared.ActionConfigUtil;
import ai.aliz.jarvis.service.shared.ActionFactoryHelperService;
import ai.aliz.jarvis.testconfig.InitActionConfig;
import ai.aliz.jarvis.testconfig.InitActionType;

import static ai.aliz.jarvis.util.JarvisConstants.BQL_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.PRE;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_PATH;
import static ai.aliz.jarvis.util.JarvisConstants.SQL_FORMAT;

@Service
public class InitActionConfigFactory {
    
    private ActionFactoryHelperService actionFactoryHelperService = new ActionFactoryHelperService();
    
    public List<InitActionConfig> getInitActionConfigs(ContextLoader contextLoader, Map<String, Object> defaultProperties, File testCaseFolder) {
        Path preFolder = actionFactoryHelperService.getTargetFolderPath(testCaseFolder, PRE);
        List<InitActionConfig> initActions;
        try {
            initActions = Files.list(preFolder).flatMap(initActionFile -> {
                
                List<InitActionConfig> initActionConfigs = Lists.newArrayList();
                
                String fileName = initActionFile.toFile().getName();
                if (Files.isRegularFile(initActionFile)) {
                    String baseName = FilenameUtils.getBaseName(fileName);
                    Preconditions.checkNotNull(contextLoader.getContext(baseName), "No context exists with name: %s", baseName);
    
                    InitActionConfig initActionConfig = new InitActionConfig();
                    initActionConfig.setSystem(baseName);
                    initActionConfig.setDescriptorFolder(null);
    
                    String extension = FilenameUtils.getExtension(fileName);
                    switch (extension) {
                        case BQL_FORMAT:
                        case SQL_FORMAT:
                            initActionConfig.setType(InitActionType.SQLExec);
                            initActionConfig.getProperties().put(SOURCE_PATH, initActionFile.toFile().getAbsolutePath());
                            break;
                        default:
                            throw new UnsupportedOperationException("Not supported extension for init action autodetect: " + initActionFile);
                    }
                    initActionConfigs.add(initActionConfig);
                } else {
                    Context context = actionFactoryHelperService.getContext(contextLoader, fileName);
                    String system = fileName;
                    
                    ContextType contextType = context.getContextType();
                    switch (contextType) {
                        case SFTP:
                            initActionConfigs.add(ActionConfigUtil.getInitActionConfigForSFTP(initActionFile, system));
                            break;
                        case BigQuery:
                            for (File datasetFolder : initActionFile.toFile().listFiles()) {
                                Preconditions.checkArgument(datasetFolder.isDirectory(), "%s is not a directory", datasetFolder);
                                String datasetName = datasetFolder.getName();
                                for (File tableJsonFile : datasetFolder.listFiles()) {
                                    Preconditions.checkArgument(tableJsonFile.isFile());
                                    initActionConfigs.add(ActionConfigUtil.getInitActionConfigForBq(defaultProperties, context.getId(), system, datasetName, tableJsonFile));
                                }
                            }
                            break;
                        default:
                            throw new UnsupportedOperationException(String.format("Not supported context type %s for folder %s", contextType, fileName));
                    }
                }
                return initActionConfigs.stream();
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
        }
        return initActions;
    }
}
