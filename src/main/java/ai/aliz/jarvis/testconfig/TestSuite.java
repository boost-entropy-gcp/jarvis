package ai.aliz.jarvis.testconfig;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@ToString(exclude = "parentSuite")
public class TestSuite {

    private Boolean caseAutoDetect;

    private final List<TestCase> testCases = new ArrayList<>();
    
    private String configPath;
    
    private String rootFolder;
    
    private Map<String, Object> properties;
    
    private TestSuite parentSuite;
    
    private final List<TestSuite> suites = new ArrayList<>();
}
