package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.context.ContextType;
import ai.aliz.talendtestrunner.testconfig.InitActionConfig;
import ai.aliz.talendtestrunner.testconfig.InitActionType;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ai.aliz.talendtestrunner.helper.Helper.SOURCE_PATH;

@Service
public class InitActionConfigCreator {

    private ActionCreatorHelperService actionCreatorHelperService = new ActionCreatorHelperService();

    private ActionConfigForBq actionConfigForBq = new ActionConfigForBq();

    public List<InitActionConfig> getInitActionConfigs(ContextLoader contextLoader, Map<String, Object> defaultProperties, File testCaseFolder) {
        Path preFolder = actionCreatorHelperService.getTargetFolderPath(testCaseFolder, "pre");
        List<InitActionConfig> initActions = null;
        try {
            initActions = Files.list(preFolder).flatMap(initActionFile -> {

                List<InitActionConfig> initActionConfigs = Lists.newArrayList();

                String fileName = initActionFile.toFile().getName();
                if (Files.isRegularFile(initActionFile)) {
                    InitActionConfig initActionConfig = new InitActionConfig();
                    String baseName = FilenameUtils.getBaseName(fileName);
                    Preconditions.checkNotNull(contextLoader.getContext(baseName), "No context exists with name: %s", baseName);

                    initActionConfig.setSystem(baseName);

                    String extension = FilenameUtils.getExtension(fileName);

                    initActionConfig.setDescriptorFolder(null);

                    switch (extension) {
                        case "bql":
                        case "sql":
                            initActionConfig.setType(InitActionType.SQLExec);
                            initActionConfig.getProperties().put(SOURCE_PATH, initActionFile.toFile().getAbsolutePath());
                            break;
                        default:
                            throw new UnsupportedOperationException("Not supported extension for init action autodetect: " + initActionFile);

                    }
                    initActionConfigs.add(initActionConfig);
                } else {
                    Context context = actionCreatorHelperService.getContext(contextLoader, fileName);
                    String system = fileName;

                    ContextType contextType = context.getContextType();
                    switch (contextType) {
                        case SFTP:
                            initActionConfigs.add(getInitActionConfigForSFTP(initActionFile, system));
                            break;
                        case BigQuery:
                            for (File datasetFolder : initActionFile.toFile().listFiles()) {
                                Preconditions.checkArgument(datasetFolder.isDirectory(), "%s is not a directory", datasetFolder);
                                String datasetName = datasetFolder.getName();
                                for (File tableJsonFile : datasetFolder.listFiles()) {
                                    Preconditions.checkArgument(tableJsonFile.isFile());
                                    initActionConfigs.add(actionConfigForBq.getInitActionConfigForBq(defaultProperties, context.getId(), system, datasetName, tableJsonFile));
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
            throw new RuntimeException(e);
        }
        return initActions;
    }

    private static InitActionConfig getInitActionConfigForSFTP(Path initActionFile, String system) {
        InitActionConfig initActionConfig = new InitActionConfig();
        initActionConfig.setType(InitActionType.SFTPLoad);
        initActionConfig.setSystem(system);
        initActionConfig.getProperties().put(SOURCE_PATH, initActionFile.toFile().getAbsolutePath());
        return initActionConfig;
    }
}
