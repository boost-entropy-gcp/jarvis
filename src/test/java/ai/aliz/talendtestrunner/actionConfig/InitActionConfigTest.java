package ai.aliz.talendtestrunner.actionConfig;

import ai.aliz.talendtestrunner.service.ActionConfigForBq;
import ai.aliz.talendtestrunner.service.AssertServiceTest;
import ai.aliz.talendtestrunner.service.InitActionConfigCreator;
import ai.aliz.jarvis.testconfig.InitActionConfig;

import com.google.cloud.bigquery.BigQuery;
import lombok.SneakyThrows;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Ignore
public class InitActionConfigTest {
    
    private ActionConfigForBq actionConfigForBq = new ActionConfigForBq();
    
    private InitActionConfigCreator initActionConfigCreator = new InitActionConfigCreator();
    
    @MockBean
    private BigQuery bigQuery;
    
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
//        Map<String, Object> defaultP = new HashMap<>();
//        ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
//        String contextPath = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test_context.json").getFile())).getPath();
//        contextLoader.parseContext(contextPath);
//        String configPath = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("test_structure").getFile())).getPath() + File.separatorChar + "test_json";
//
//        List<InitActionConfig> initActionConfigs = initActionConfigCreator.getInitActionConfigs(contextLoader, defaultP, new File(configPath));
//
//        InitActionConfig initActionConfig1 = initActionConfigs.stream().filter(config -> config.getType().equals(InitActionType.BQLoad)).findFirst().get();
//        InitActionConfig initActionConfig2 = initActionConfigs.stream().filter(config -> config.getType().equals(InitActionType.SQLExec)).findFirst().get();
//
//        assertThat(initActionConfig1.getType(), is(InitActionType.BQLoad));
//        assertThat(initActionConfig1.getProperties().get("sourcePath"), is(configPath + TestHelper.addSeparator("\\pre\\TEST_ID\\test_dataset\\init.json")));
//        assertThat(initActionConfig1.getSystem(), is("TEST_ID"));
//
//        assertThat(initActionConfig2.getType(), is(InitActionType.SQLExec));
//        assertThat(initActionConfig2.getProperties().get("sourcePath"), is(configPath + TestHelper.addSeparator("\\pre\\TEST_ID.sql")));
//        assertThat(initActionConfig2.getSystem(), is("TEST_ID"));
    }
}
