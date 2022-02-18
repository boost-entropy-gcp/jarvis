package ai.aliz.jarvis.testconfig;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestCase {
    
    private boolean autoDetected;

    private String name;
    
    private String path;
    
    private List<InitActionConfig> initActionConfigs = new ArrayList<>();

    private List<ExecutionActionConfig> executionActionConfigs = new ArrayList<>();

    private List<AssertActionConfig> assertActionConfigs = new ArrayList<>();
}
