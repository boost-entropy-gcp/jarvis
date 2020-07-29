package ai.aliz.talendtestrunner.testcase;

import ai.aliz.talendtestrunner.util.TestCollector;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;

@Data
public class TestCase {
    private String jobName;
    private String group;
    private Path baseFolder;
    private String caseId;

    private List<Path> preparationFiles;
    private List<TestCollector.AssertionDefition> assertionDefinitions;

}
