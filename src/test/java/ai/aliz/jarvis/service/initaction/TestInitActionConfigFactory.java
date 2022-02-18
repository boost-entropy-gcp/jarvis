package ai.aliz.jarvis.service.initaction;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ai.aliz.jarvis.testconfig.InitActionConfigFactory;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.testconfig.InitActionConfig;
import ai.aliz.jarvis.testconfig.InitActionType;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static ai.aliz.jarvis.util.JarvisConstants.DATASET;
import static ai.aliz.jarvis.util.JarvisConstants.JSON_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.NO_METADAT_ADDITION;
import static ai.aliz.jarvis.util.JarvisConstants.PRE;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_PATH;
import static ai.aliz.jarvis.util.JarvisConstants.TABLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = "context=src/test/resources/init/init-contexts.json")
@ContextConfiguration(classes = {TestContextLoader.class, InitActionConfigFactory.class})
public class TestInitActionConfigFactory {
    
    @Autowired
    private TestContextLoader contextLoader;
    
    @Autowired
    private InitActionConfigFactory initActionConfigFactory;
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    @Test
    public void testGetInitActionConfigsMissingFolder() {
        String relativePath = "src/test/resources/init/invalid";
        File file = new File(relativePath);
        Path preFolderPath = Paths.get(file.getAbsolutePath(), PRE);
        
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("pre folder does not exists " + preFolderPath.toString());
    
        initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File(relativePath));
    }
    
    @Test
    public void testGetInitActionConfigsSQLExecFromBQL() {
        String relativeFilePath = "src/test/resources/init/bql/pre/bql.bql";
        File file = new File(relativeFilePath);
        
        InitActionConfig expected = new InitActionConfig();
        expected.setSystem("bql");
        expected.setType(InitActionType.SQLExec);
        expected.getProperties().put(SOURCE_PATH,file.getAbsolutePath());

        List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/init/bql"));
        
        assertTrue(actionConfigs.contains(expected));
        assertEquals(1, actionConfigs.size());
    }
    
    @Test
    public void testGetInitActionConfigsSQLExecFromSQL() {
        String relativeFilePath = "src/test/resources/init/sql/pre/sql.sql";
        File file = new File(relativeFilePath);
    
        InitActionConfig expected = new InitActionConfig();
        expected.setSystem("sql");
        expected.setType(InitActionType.SQLExec);
        expected.getProperties().put(SOURCE_PATH,file.getAbsolutePath());
    
        List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/init/sql"));
    
        assertTrue(actionConfigs.contains(expected));
        assertEquals(1, actionConfigs.size());
    }
    
    @Test
    public void testGetInitActionConfigsBQLoadFromJSON() {
        String relativeFilePath = "src/test/resources/init/bq/pre/bq/dataset/table.json";
        File file = new File(relativeFilePath);
    
        InitActionConfig expected = new InitActionConfig();
        expected.setSystem("bq");
        expected.setType(InitActionType.BQLoad);
        Map<String, Object> properties = expected.getProperties();
        properties.put(SOURCE_PATH, file.getAbsolutePath());
        properties.put(NO_METADAT_ADDITION, true);
        properties.put(DATASET, "dataset");
        properties.put(SOURCE_FORMAT, JSON_FORMAT);
        properties.put(TABLE, "table");
    
        List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/init/bq"));
    
        assertTrue(actionConfigs.contains(expected));
        assertEquals(1, actionConfigs.size());
    }
    
    @Test
    public void testGetInitActionConfigsSFTP() {
        String relativeFilePath = "src/test/resources/init/sftp/pre/sftp";
        File file = new File(relativeFilePath);
    
        InitActionConfig expected = new InitActionConfig();
        expected.setSystem("sftp");
        expected.setType(InitActionType.SFTPLoad);
        Map<String, Object> properties = expected.getProperties();
        properties.put(SOURCE_PATH, file.getAbsolutePath());
    
        List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/init/sftp"));
    
        assertTrue(actionConfigs.contains(expected));
        assertEquals(1, actionConfigs.size());
    }
    
    @Test
    public void testGetInitActionConfigsNotSupported() {
        String relativeFilePath = "src/test/resources/init/not-supported/pre/bq.json";
        File file = new File(relativeFilePath);
    
        exceptionRule.expect(UnsupportedOperationException.class);
        exceptionRule.expectMessage("Not supported extension for init action autodetect: " + file.getAbsolutePath());
    
        initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/init/not-supported"));
    }
}
