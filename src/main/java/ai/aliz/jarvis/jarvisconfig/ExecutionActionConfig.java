package ai.aliz.jarvis.jarvisconfig;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ExecutionActionConfig implements StepConfig {


    private String executionContext;
    private ExecutionType type;

    private String descriptorFolder;

    private final Map<String, Object> properties = new HashMap<>();
}
