package ai.aliz.jarvis.service.shared;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Lists;

import ai.aliz.jarvis.config.AssertActionConfig;
import ai.aliz.jarvis.config.InitActionConfig;
import ai.aliz.jarvis.config.InitActionType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static ai.aliz.jarvis.util.JarvisConstants.ASSERT_KEY_COLUMNS;
import static ai.aliz.jarvis.util.JarvisConstants.DATASET;
import static ai.aliz.jarvis.util.JarvisConstants.EXCLUDE_PREVIOUSLY_INSERTED_ROWS;
import static ai.aliz.jarvis.util.JarvisConstants.JSON_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.NO_METADAT_ADDITION;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_FORMAT;
import static ai.aliz.jarvis.util.JarvisConstants.SOURCE_PATH;
import static ai.aliz.jarvis.util.JarvisConstants.TABLE;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class TestActionConfigUtil {
    
    @Test
    public void testGetInitActionConfigForSFTP() {
        String relativePath = "src/test/resources/shared/sftp-init.csv";
        File file = new File(relativePath);
        String absolutePath = file.getAbsolutePath();
        Path path = Paths.get(absolutePath);
        
        InitActionConfig expected = new InitActionConfig();
        expected.setType(InitActionType.SFTPLoad);
        expected.setSystem("sftp-init.csv");
        expected.getProperties().put(SOURCE_PATH, absolutePath);
        
        assertEquals(expected, ActionConfigUtil.getInitActionConfigForSFTP(path, file.getName()));
    }
    
    @Test
    public void testGetInitActionConfigForBq() {
        String relativePath = "src/test/resources/shared/bq-init.json";
        File file = new File(relativePath);
        String contextId = "contextId";
    
        InitActionConfig expected = new InitActionConfig();
        expected.setSystem("bq-init.json");
        expected.setType(InitActionType.BQLoad);
        String absolutePath = file.getAbsolutePath();
        Map<String, Object> properties = new HashMap<>();
        properties.put(SOURCE_PATH, absolutePath);
        properties.put(DATASET, "datasetName");
        properties.put(TABLE, "bq-init");
        properties.put(SOURCE_FORMAT, JSON_FORMAT);
        properties.put(NO_METADAT_ADDITION, true);
        expected.getProperties().putAll(properties);
    
        assertEquals(expected, ActionConfigUtil.getInitActionConfigForBq(new HashMap<>(), contextId, file.getName(), "datasetName", file));
    }
    
    @Test
    public void testGetAssertActionConfigForBq() {
        String relativePath = "src/test/resources/shared/bq-assert.json";
        File file = new File(relativePath);
    
        AssertActionConfig expected = new AssertActionConfig();
        expected.setSystem("bq-assert.json");
        expected.setType("AssertDataEquals");
        Map<String, Object> properties = new HashMap<>();
        properties.put(SOURCE_PATH, file.getAbsolutePath());
        properties.put(DATASET, "datasetName");
        properties.put(TABLE, "bq-assert");
        properties.put(SOURCE_FORMAT, JSON_FORMAT);
        properties.put(ASSERT_KEY_COLUMNS, Lists.newArrayList("bq-assert_BID", "bq-assert_VALID_FROM"));
        properties.put(EXCLUDE_PREVIOUSLY_INSERTED_ROWS, false);
        expected.getProperties().putAll(properties);
    
        assertEquals(expected, ActionConfigUtil.getAssertActionConfigForBq(new HashMap<>(), file.getName(), "datasetName", file));
    }
}
