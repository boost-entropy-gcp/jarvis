package ai.aliz.talendtestrunner.service;

import ai.aliz.jarvis.config.AssertActionConfig;
import ai.aliz.jarvis.util.JarvisUtil;
import com.google.cloud.bigquery.BigQuery;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.Objects;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Ignore
public class AssertServiceTest {

    @MockBean
    private BigQuery bigQuery;

    @Test
    public void testReadAssertJsonFile() {
        AssertActionConfig assertActionConfig = new AssertActionConfig();
        String path = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test.json").getFile())).getPath();
        assertActionConfig.getProperties().put("sourcePath", path);
        String expected = "[{\"name\": \"test\", \"test\": \"test\"}]";
        String actual = JarvisUtil.getSourceContentFromConfigProperties(assertActionConfig);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReadAssertJsonFileWithAssertProperties() {
        AssertActionConfig assertActionConfig = new AssertActionConfig();
        String path = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("assertTestWithProperties.json").getFile())).getPath();
        assertActionConfig.getProperties().put("sourcePath", path);
        String expected = "[{\"name\":\"test\",\"test\":\"test\"}]";
        String actual = JarvisUtil.getSourceContentFromConfigProperties(assertActionConfig);
        Assert.assertEquals(expected, actual);
    }
}
