package ai.aliz.jarvis.testconfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContextLoader;

import static ai.aliz.talendtestrunner.helper.Helper.SOURCE_PATH;

@Service
public class ExecutionActionConfigFactory {
    
    public static final String EXECUTIONS_KEY = "executions";
    
    @Autowired
    private TestContextLoader contextLoader;
    
    public List<ExecutionActionConfig> getExecutionActionConfig(Map testSuiteMap) {
        Object executeActionJson = testSuiteMap.get(EXECUTIONS_KEY);
        if (executeActionJson != null) {
            List<Map<String, String>> executionActions = (List<Map<String, String>>) testSuiteMap.getOrDefault(EXECUTIONS_KEY, Collections.singletonMap("type", "noOps"));
            List<ExecutionActionConfig> executionActionConfigs = getExecutionActionConfigs(executionActions);
            return executionActionConfigs;
        }
        
        return new ArrayList<>();
    }
    
    private List<ExecutionActionConfig> getExecutionActionConfigs(List<Map<String, String>> executions) {
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
