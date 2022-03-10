package ai.aliz.jarvis.jarvisconfig;

import java.util.Map;

public interface StepConfig {
    
    String getDescriptorFolder();
    
    Map<String, Object> getProperties();
}
