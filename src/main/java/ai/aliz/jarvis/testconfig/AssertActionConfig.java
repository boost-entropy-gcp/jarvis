package ai.aliz.jarvis.testconfig;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class AssertActionConfig implements StepConfig {
    
    private String system;
    private String type;
    private String descriptorFolder;
    private Map<String, Object> properties = new HashMap<>();
    
   
}
