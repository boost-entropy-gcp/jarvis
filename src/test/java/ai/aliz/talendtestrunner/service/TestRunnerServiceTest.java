package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.testcase.TestCase;
import ai.aliz.talendtestrunner.util.TestCollector;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.util.Collection;

@RunWith(Parameterized.class)
@SpringBootTest
@Ignore
public class TestRunnerServiceTest {
    private static final String API_URL = "apiUrl";

    @Autowired
    private TestRunnerService testRunnerService;

    @Autowired
    private ContextLoader contextLoader;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameters(name = "{0}")
    public static Collection jobNames() {
        return new TestCollector().listTestCases();
    }

    private TestCase testCase;

    public TestRunnerServiceTest(TestCase testCase) {
        this.testCase = testCase;
    }

    @Test
    public void runTestForJob() throws Exception {
        testRunnerService.runTest(testCase);
    }


}
