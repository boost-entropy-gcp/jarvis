package ai.aliz.talendtestrunner.testconfig;

import java.util.Map;

public interface StepConfig {
    
    String getDescriptorFolder();
    
    Map<String, Object> getProperties();
}
