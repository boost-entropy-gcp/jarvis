package ai.aliz.talendtestrunner.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.testconfig.StepConfig;

@UtilityClass
public class TestRunnerUtil {
    
    @SneakyThrows
    public String getSourceContentFromConfigProperties(StepConfig stepConfig) {
        
        String sourcePath = (String) stepConfig.getProperties().get("sourcePath");
        File sourceFile;
        if (Paths.get(sourcePath).isAbsolute()) {
            sourceFile = new File(sourcePath);
            
        } else {
            sourceFile = new File(stepConfig.getDescriptorFolder() + sourcePath);
        }
        
        return IOUtils.toString(sourceFile.toURI(), StandardCharsets.UTF_8);
    }
    
    public String getDatasetName(Map<String, Object> properties, Context context) {
        String dataset = (String) properties.get("dataset");
        String datasetNamePrefix = context.getParameter("datasetNamePrefix");
        if (datasetNamePrefix != null) {
            dataset = datasetNamePrefix + dataset;
        }
        
        return dataset;
    }
}
