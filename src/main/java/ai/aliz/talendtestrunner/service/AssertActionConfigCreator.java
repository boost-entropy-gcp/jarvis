package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import com.google.common.base.Preconditions;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ai.aliz.talendtestrunner.helper.Helper.SOURCE_PATH;

@Service
public class AssertActionConfigCreator {

    private ActionCreatorHelperService actionCreatorHelperService = new ActionCreatorHelperService();

    private ActionConfigForBq actionConfigForBq = new ActionConfigForBq();

    public List<AssertActionConfig> getAssertActionConfigs(ContextLoader contextLoader, Map<String, Object> defaultProperties, File testCaseFolder) {
        Path assertFolder = actionCreatorHelperService.getTargetFolderPath(testCaseFolder, "assert");
        List<AssertActionConfig> assertActionConfigs = null;
        try {
            assertActionConfigs = Files.list(assertFolder).flatMap(assertActionConfigPath -> {
                List<AssertActionConfig> assertActionConfigsForFolder = new ArrayList<>();
                if (Files.isDirectory(assertActionConfigPath)) {

                    File assertContextFolder = assertActionConfigPath.toFile();
                    String directoryName = assertContextFolder.getName();
                    Context context = actionCreatorHelperService.getContext(contextLoader, directoryName);
                    String system = context.getId();

                    switch (context.getContextType()) {
                        case BigQuery:
                            for (File datasetFolder : assertContextFolder.listFiles()) {
                                Preconditions.checkArgument(Files.isDirectory(datasetFolder.toPath()),
                                        "The context folder should contain only directories for the different datasets: %s",
                                        datasetFolder);
                                String datasetName = datasetFolder.getName();
                                for (File tableDataFile : datasetFolder.listFiles()) {
                                    Preconditions.checkArgument(Files.isRegularFile(tableDataFile.toPath()),
                                            "The dataset folder should contain only json files for the different tables: %s",
                                            tableDataFile);
                                    assertActionConfigsForFolder.add(actionConfigForBq.getAssertActionConfigForBq(defaultProperties, system, datasetName, tableDataFile));
                                }
                            }
                            break;
                        case MySQL:
                            for (File tableDataFile : assertContextFolder.listFiles()) {
                                Preconditions.checkArgument(Files.isRegularFile(tableDataFile.toPath()),
                                        "The assert folder should contain only json files for the different tables: %s",
                                        tableDataFile);
                                assertActionConfigsForFolder.add(getAssertActionConfigForMySQL(system, tableDataFile));
                            }
                            break;
                        default:
                            throw new UnsupportedOperationException("Not supported context type for assert " + context.getContextType());
                    }
                } else {
                    throw new UnsupportedOperationException("Files are not supported in assert autodetect: " + assertActionConfigPath);
                }

                return assertActionConfigsForFolder.stream();
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return assertActionConfigs;
    }

    private static AssertActionConfig getAssertActionConfigForMySQL(String system, File tableDataFile) {
        AssertActionConfig assertActionConfig = new AssertActionConfig();
        assertActionConfig.setType("AssertTalendJobState");
        assertActionConfig.setSystem(system);
        assertActionConfig.getProperties().put(SOURCE_PATH, tableDataFile.getAbsolutePath());
        return assertActionConfig;
    }
}
