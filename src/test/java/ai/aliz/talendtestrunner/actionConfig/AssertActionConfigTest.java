package ai.aliz.talendtestrunner.actionConfig;

import ai.aliz.talendtestrunner.service.ActionConfigForBq;
import ai.aliz.talendtestrunner.service.AssertServiceTest;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class AssertActionConfigTest {

    @Test
    public void createBqAssertActionConfigTest() {
        Map<String, Object> defaultP = new HashMap<>();
        doTest(defaultP);
    }

    @Test
    public void createBqAssertActionConfigWithDefaultPropertiesTest() {
        Map<String, Object> defaultP = new HashMap<>();
        defaultP.put("assert.assertKeyColumns", "testProperty");
        AssertActionConfig assertActionConfig = doTest(defaultP);

        Assert.assertThat(assertActionConfig.getProperties().get("assertKeyColumns"), is("testProperty"));
    }

    private AssertActionConfig doTest(Map<String, Object> defaultP) {
        File file = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("assertTest.json").getFile()));
        AssertActionConfig assertActionConfig = ActionConfigForBq.getAssertActionConfigForBq(defaultP, "testSystem", "testDataset", file);

        Assert.assertThat(assertActionConfig.getSystem(), is("testSystem"));
        Assert.assertThat(assertActionConfig.getType(), is("AssertDataEquals"));
        Assert.assertThat(assertActionConfig.getProperties().get("dataset"), is("testDataset"));
        Assert.assertThat(assertActionConfig.getProperties().get("sourcePath"), is(file.getPath()));

        return assertActionConfig;
    }
}
