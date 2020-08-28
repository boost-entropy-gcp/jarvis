package ai.aliz.talendtestrunner.actionConfig;

import ai.aliz.talendtestrunner.IntegrationTestRunner;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.ActionConfigForBq;
import ai.aliz.talendtestrunner.service.AssertServiceTest;
import ai.aliz.talendtestrunner.service.InitActionConfigCreator;
import ai.aliz.talendtestrunner.testconfig.InitActionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class InitActionConfigTest {

    private ActionConfigForBq actionConfigForBq = new ActionConfigForBq();

    private InitActionConfigCreator initActionConfigCreator = new InitActionConfigCreator();

    @Test
    public void createInitActionConfigForBqTest() {
        Map<String, Object> defaultP = new HashMap<>();
        File file = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test.json").getFile()));
        InitActionConfig initActionConfig = actionConfigForBq.getInitActionConfigForBq(defaultP, "contextId", "system", "dataset", file);

        assertThat(initActionConfig.getSystem(), is("system"));
        assertThat(initActionConfig.getProperties().get("sourceFormat"), is("json"));
        assertThat(initActionConfig.getProperties().get("sourcePath"), is(file.getPath()));
    }

    @Test
    @SneakyThrows
    public void testCreateInitActionList() {
        Map<String, Object> defaultP = new HashMap<>();
        ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
        String contextPath = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test_context.json").getFile())).getPath();
        contextLoader.parseContext(contextPath);
        String configPath = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test_structure").getFile())).getPath() + "\\test_json";

        List<InitActionConfig> initActionConfigs = initActionConfigCreator.getInitActionConfigs(contextLoader, defaultP, new File(configPath));

        InitActionConfig initActionConfig1 = initActionConfigs.get(0);
        InitActionConfig initActionConfig2 = initActionConfigs.get(1);

        assertThat(initActionConfig1.getProperties().get("sourcePath"), is(configPath + "\\pre\\TEST_ID\\test_dataset\\init.json"));
        assertThat(initActionConfig1.getType(), is("BQLoad"));
        assertThat(initActionConfig1.getSystem(), is("TEST_ID"));

        assertThat(initActionConfig2.getProperties().get("sourcePath"), is(configPath + "\\pre\\TEST_ID.sql"));
        assertThat(initActionConfig2.getType(), is("SQLExec"));
        assertThat(initActionConfig2.getSystem(), is("TEST_ID"));
    }
}
