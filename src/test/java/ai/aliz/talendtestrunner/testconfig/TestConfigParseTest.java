package ai.aliz.talendtestrunner.testconfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aliz.talendtestrunner.context.ContextLoader;

import org.junit.Test;

public class TestConfigParseTest {
    
    @Test
    public void testConfig() throws Exception {
        
        final ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
        contextLoader.parseContext("");
        
        TestSuite testSuite = TestSuite.readTestConfig("", contextLoader);
        
        System.out.println(testSuite);
    }
}
