package ai.aliz.jarvis.config;

import lombok.Lombok;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.JarvisContextLoader;

import static ai.aliz.jarvis.util.Helper.SOURCE_PATH;

@Service
public class ExecutionActionConfigFactory {
    
    public static final String EXECUTIONS_KEY = "executions";
    
    @Autowired
    private JarvisContextLoader contextLoader;
    
    public List<ExecutionActionConfig> getExecutionActionConfig(Map jarvisTestSuiteMap) {
        Object executeActionJson = jarvisTestSuiteMap.get(EXECUTIONS_KEY);
        if (executeActionJson != null) {
            List<Map<String, String>> executionActions = (List<Map<String, String>>) jarvisTestSuiteMap.getOrDefault(EXECUTIONS_KEY, Collections.singletonMap("type", "noOps"));
            List<ExecutionActionConfig> executionActionConfigs = getExecutionActionConfigs(executionActions);
            return executionActionConfigs;
        }
        
        return new ArrayList<>();
    }
    
    private List<ExecutionActionConfig> getExecutionActionConfigs(List<Map<String, String>> executions) {
        final String repositoryRoot = getPath(contextLoader.getContext("local").getParameters(), "repositoryRoot");
        
        return executions.stream().map(execution -> {
            final String executionContext = getString(execution, "executionContext");
            final ExecutionType executionType = getEnum(execution, "executionType", ExecutionType.class);
            
            final ExecutionActionConfig executionActionConfig = new ExecutionActionConfig();
            executionActionConfig.setExecutionContext(contextLoader.getContext(executionContext).getId());
            executionActionConfig.setType(executionType);
            
            switch (executionActionConfig.getType()) {
                case BqQuery:
                    final String queryPath = getPath(execution, "queryPath");
                    
                    final File sourceFile = new File(repositoryRoot, queryPath);
                    Preconditions.checkState(sourceFile.isFile(), "Unable to find query file %s", sourceFile.getAbsolutePath());
                    try {
                        executionActionConfig.getProperties().put(SOURCE_PATH, sourceFile.getCanonicalPath());
                    } catch (IOException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                    return executionActionConfig;
                case Talend:
                case Airflow:
                case NoOps:
                default:
                    throw new UnsupportedOperationException("This execution type '" + executionActionConfig.getType() + "' isn't supported.");
            }
        }).collect(Collectors.toList());
    }
    
    @Nonnull
    private String getString(@NonNull Map<String, String> map, @NonNull String key) {
        final String str = map.get(key);
        Preconditions.checkState(!Strings.isNullOrEmpty(str), "Unable to find value for property: '%s'", key);
        return str;
    }
    
    @Nonnull
    private <E extends Enum<E>> E getEnum(@NonNull Map<String, String> map, @NonNull String key, Class<E> enumClass) {
        final String str = getString(map, key);
        final E e = Enums.getIfPresent(enumClass, str).orNull();
        Preconditions.checkState(e != null, "Unable to find %s enum with name: '%s'", enumClass.getSimpleName(), str);
        return e;
    }
    
    @Nonnull
    private String getPath(@NonNull Map<String, String> map, @NonNull String key) {
        final String str = getString(map, key);
        return moderateFilePathSlashes(str);
    }
    
    private String moderateFilePathSlashes(String path) {
        return path
                .replace('\\', File.separatorChar)
                .replace('/', File.separatorChar);
    }
    
}
