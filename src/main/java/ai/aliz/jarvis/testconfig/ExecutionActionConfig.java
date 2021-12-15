package ai.aliz.jarvis.testconfig;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExecutionActionConfig implements StepConfig {


    private String executionContext;
    private ExecutionType type;

    private String descriptorFolder;

    private final Map<String, Object> properties = new HashMap<>();
}
