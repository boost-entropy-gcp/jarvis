package ai.aliz.talendtestrunner.util;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.testconfig.AssertActionConfig;
import ai.aliz.jarvis.testconfig.StepConfig;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;

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
    
    public String getDatasetName(Map<String, Object> properties, TestContext context) {
        String dataset = (String) properties.get("dataset");
        String datasetNamePrefix = context.getParameter("datasetNamePrefix");
        if (datasetNamePrefix != null) {
            dataset = datasetNamePrefix + dataset;
        }
        
        return dataset;
    }
}
