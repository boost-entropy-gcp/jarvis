package ai.aliz.jarvis.config;

import java.util.Map;

public interface StepConfig {
    
    String getDescriptorFolder();
    
    Map<String, Object> getProperties();
}
