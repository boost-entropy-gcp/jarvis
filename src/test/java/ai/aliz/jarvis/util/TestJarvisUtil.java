package ai.aliz.jarvis.util;

import java.util.HashMap;
import java.util.Map;

import ai.aliz.jarvis.context.JarvisContext;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static ai.aliz.jarvis.util.JarvisConstants.DATASET;
import static ai.aliz.jarvis.util.JarvisConstants.DATASET_NAME_PREFIX;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class TestJarvisUtil {
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    @Test
    public void testResolvePlaceholders() {
        String query = "select * from {{table}} where {{condition}}={{condition}};";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("table", "REPLACED_TABLE");
        parameters.put("condition", "1");
        assertEquals("select * from REPLACED_TABLE where 1=1;", JarvisUtil.resolvePlaceholders(query, parameters));
    }
    
    @Test
    public void testResolvePlaceholdersNotPresentParam() {
        String query = "select * from {{table}} where {{column}}={{value}};";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("table", "REPLACED_TABLE");
        parameters.put("value", "1");
        
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Some placeholders have not been resolved in: 'select * from REPLACED_TABLE where {{column}}=1;'");
        JarvisUtil.resolvePlaceholders(query, parameters);
    }
    
    @Test
    public void testGetDatasetNameFromConfigProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DATASET, "dataset");
        JarvisContext context = JarvisContext.builder().parameter(DATASET_NAME_PREFIX, "core_").build();
        assertEquals("core_dataset", JarvisUtil.getDatasetNameFromConfigProperties(properties, context));
    }
    
    @Test
    public void testGetDatasetNameFromConfigPropertiesNullPrefix() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DATASET, "dataset");
        JarvisContext context = JarvisContext.builder().build();
        assertEquals("dataset", JarvisUtil.getDatasetNameFromConfigProperties(properties, context));
    }
}
