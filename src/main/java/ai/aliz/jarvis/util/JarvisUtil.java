package ai.aliz.jarvis.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import com.google.common.base.Preconditions;

import org.apache.commons.io.IOUtils;

import ai.aliz.jarvis.config.AssertActionConfig;
import ai.aliz.jarvis.config.StepConfig;
import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.context.JarvisContextLoader;

import static ai.aliz.jarvis.util.JarvisConstants.DATASET;
import static ai.aliz.jarvis.util.JarvisConstants.DATASET_NAME_PREFIX;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_PATH;

@UtilityClass
public class JarvisUtil {
    
    @SneakyThrows
    public String getSourceContentFromConfigProperties(StepConfig stepConfig) {
        
        String sourcePath = (String) stepConfig.getProperties().get(SOURCE_PATH);
        File sourceFile;
        if (Paths.get(sourcePath).isAbsolute()) {
            sourceFile = new File(sourcePath);
            
        } else {
            sourceFile = new File(stepConfig.getDescriptorFolder() + sourcePath);
        }
        
        if (stepConfig.getClass().equals(AssertActionConfig.class)) {
            JsonReader jsonReader = Json.createReader(new FileReader(sourceFile));
            JsonStructure jsonStructure = jsonReader.read();
            if (jsonStructure.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                JsonObject object = jsonStructure.asJsonObject();
                return String.valueOf(object.get("rows"));
            }
            jsonReader.close();
        }
        return IOUtils.toString(sourceFile.toURI(), StandardCharsets.UTF_8);
    }
    
    public String getDatasetNameFromConfigProperties(Map<String, Object> properties, JarvisContext context) {
        String dataset = (String) properties.get(DATASET);
        String datasetNamePrefix = context.getParameter(DATASET_NAME_PREFIX);
        if (datasetNamePrefix != null) {
            dataset = datasetNamePrefix + dataset;
        }
        return dataset;
    }
    
    @Deprecated
    public String resolvePlaceholders(String pattern, Map<String, String> parameters) {
        return PlaceholderResolver.resolve(pattern, parameters);
    }
    
    public JarvisContext getContext(JarvisContextLoader contextLoader, String contextId) {
        JarvisContext context = contextLoader.getContext(contextId);
        Preconditions.checkNotNull(context, "No context exists with name: %s", contextId);
        return context;
    }
    
    public Path getTargetFolderPath(File jarvisTestCaseFolder, String folderName) {
        Path folderPath = Paths.get(jarvisTestCaseFolder.getAbsolutePath(), folderName);
        Preconditions.checkArgument(Files.isDirectory(folderPath), "%s folder does not exists %s", folderName, folderPath);
        return folderPath;
    }
    
    public String getDatasetName(Map<String, Object> properties, JarvisContext context) {
        String dataset = (String) properties.get("dataset");
        String datasetNamePrefix = context.getParameter("datasetNamePrefix");
        if (datasetNamePrefix != null) {
            dataset = datasetNamePrefix + dataset;
        }
        
        return dataset;
    }
}

