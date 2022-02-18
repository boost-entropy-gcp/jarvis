package ai.aliz.jarvis.testconfig;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class InitActionConfig implements StepConfig {
    
    private String system;
    private InitActionType type;
    
    private String descriptorFolder;
    
    private final Map<String, Object> properties = new HashMap<>();
    
}
