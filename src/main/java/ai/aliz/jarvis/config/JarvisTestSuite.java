package ai.aliz.jarvis.config;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@ToString(exclude = "parentSuite")
public class JarvisTestSuite {

    private Boolean caseAutoDetect;

    private final List<JarvisTestCase> jarvisTestCases = new ArrayList<>();
    
    private String configPath;
    
    private String rootFolder;
    
    private Map<String, Object> properties;
    
    private JarvisTestSuite parentSuite;
    
    private final List<JarvisTestSuite> suites = new ArrayList<>();
}
