package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;
import ai.aliz.talendtestrunner.testconfig.ExecutionType;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ai.aliz.talendtestrunner.helper.Helper.SOURCE_PATH;


public class ExecutionActionConfigCreator {

    public List<ExecutionActionConfig> getExecutionActionConfigs(ContextLoader contextLoader, List<Map<String, String>> executions) {
        List<ExecutionActionConfig> executionActionConfigs = Lists.newArrayList();

        String repositoryRoot = moderateFilePathSlashes(contextLoader.getContext("local").getParameter("repositoryRoot"));
        return executions.stream()
                .flatMap(e -> {
                    ExecutionActionConfig executionActionConfig = new ExecutionActionConfig();
                    executionActionConfig.setType(ExecutionType.valueOf(checkExecutionType(e.get("executionType"))));
                    executionActionConfig.getProperties().put(SOURCE_PATH, repositoryRoot + e.get("queryPath"));
                    executionActionConfig.setExecutionContext(Objects.requireNonNull(e.get("executionContext"), "executionContext property must be specified on BqQuery executions"));
                    executionActionConfigs.add(executionActionConfig);

                    return executionActionConfigs.stream();
                })
                .collect(Collectors.toList());
    }

    private String moderateFilePathSlashes(String path) {
        path.replace('\\', File.separatorChar);
        path.replace('/', File.separatorChar);

        return path;
    }

    private String checkExecutionType(String executionType) {
        try {
            ExecutionType.valueOf(executionType);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Execution type %s dose not exists", executionType));
        }
        return executionType;
    }
}
