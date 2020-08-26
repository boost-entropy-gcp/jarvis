package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.Objects;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class AssertServiceTest {

    @Test
    public void testReadAssertJsonFile() {
        AssertActionConfig assertActionConfig = new AssertActionConfig();
        String path = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("assertTest.json").getFile())).getPath();
        assertActionConfig.getProperties().put("sourcePath", path);
        String expected = "{\r\n" +
                "  \"name\": \"test\",\r\n" +
                "  \"test\": \"test\"\r\n" +
                "}";
        String actual = TestRunnerUtil.getSourceContentFromConfigProperties(assertActionConfig);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReadAssertJsonFileWithAssertProperties() {
        AssertActionConfig assertActionConfig = new AssertActionConfig();
        String path = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("assertTestWithProperties.json").getFile())).getPath();
        assertActionConfig.getProperties().put("sourcePath", path);
        String expected = "[{\"test\":\"test\",\"name\":\"test\"}]";
        String actual = TestRunnerUtil.getSourceContentFromConfigProperties(assertActionConfig);
        Assert.assertEquals(expected, actual);
    }
}
