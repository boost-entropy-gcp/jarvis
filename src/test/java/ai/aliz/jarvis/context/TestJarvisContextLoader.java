package ai.aliz.jarvis.context;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import org.mockito.Mockito;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.ConfigurableEnvironment;

import ai.aliz.jarvis.util.JarvisConstants;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static ai.aliz.jarvis.util.JarvisConstants.API_KEY;
import static ai.aliz.jarvis.util.JarvisConstants.API_URL;
import static ai.aliz.jarvis.util.JarvisConstants.DATABASE;
import static ai.aliz.jarvis.util.JarvisConstants.DATASET_NAME_PREFIX;
import static ai.aliz.jarvis.util.JarvisConstants.ENVIRONMENT;
import static ai.aliz.jarvis.util.JarvisConstants.HOST;
import static ai.aliz.jarvis.util.JarvisConstants.PASSWORD;
import static ai.aliz.jarvis.util.JarvisConstants.PORT;
import static ai.aliz.jarvis.util.JarvisConstants.PROJECT;
import static ai.aliz.jarvis.util.JarvisConstants.REMOTE_BASE_PATH;
import static ai.aliz.jarvis.util.JarvisConstants.REPOSITORY_ROOT;
import static ai.aliz.jarvis.util.JarvisConstants.USER;
import static ai.aliz.jarvis.util.JarvisConstants.WORKSPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestJarvisContextLoader {
    
    private static final JarvisContext BQ_CONTEXT = JarvisContext.builder()
                                                                 .id("BQ")
                                                                 .contextType(JarvisContextType.BigQuery)
                                                                 .parameter(PROJECT, "nora-ambroz-sandbox")
                                                                 .parameter(DATASET_NAME_PREFIX, "core_")
                                                                 .parameter("stagingDataset", "STAGING")
                                                                 .build();
    
    private static final JarvisContext LOCAL_CONTEXT = JarvisContext.builder()
                                                                    .id("local")
                                                                    .contextType(JarvisContextType.LocalContext)
                                                                    .parameter(REPOSITORY_ROOT, "C:\\ds")
                                                                    .build();
    
    private static final JarvisContext MSSQL_CONTEXT = JarvisContext.builder()
                                                                    .id("MSSQL")
                                                                    .contextType(JarvisContextType.MSSQL)
                                                                    .parameter(HOST, "host")
                                                                    .parameter(PORT, "port")
                                                                    .parameter(DATABASE, "database")
                                                                    .parameter(USER, "user")
                                                                    .parameter(PASSWORD, "password")
                                                                    .build();
    
    private static final JarvisContext MYSQL_CONTEXT = JarvisContext.builder()
                                                                    .id("MySQL")
                                                                    .contextType(JarvisContextType.MySQL)
                                                                    .parameter(HOST, "host")
                                                                    .parameter(PORT, "port")
                                                                    .parameter(DATABASE, "database")
                                                                    .parameter(USER, "user")
                                                                    .parameter(PASSWORD, "password")
                                                                    .build();
    
    private static final JarvisContext SFTP_CONTEXT = JarvisContext.builder()
                                                                   .id("SFTP")
                                                                   .contextType(JarvisContextType.SFTP)
                                                                   .parameter(HOST, "host")
                                                                   .parameter(PORT, "port")
                                                                   .parameter(USER, "user")
                                                                   .parameter(PASSWORD, "password")
                                                                   .parameter(REMOTE_BASE_PATH, "/out")
                                                                   .build();
    
    private static final JarvisContext TALEND_API_CONTEXT = JarvisContext.builder()
                                                                         .id("TalendAPI")
                                                                         .contextType(JarvisContextType.TalendAPI)
                                                                         .parameter(API_URL, "url")
                                                                         .parameter(API_KEY, "key")
                                                                         .parameter(ENVIRONMENT, "environment")
                                                                         .parameter(WORKSPACE, "workspace")
                                                                         .build();
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    private JarvisContextLoader initContextLoader(String contextPath) {
        
        ApplicationArguments mockedArguments = Mockito.mock(ApplicationArguments.class);
        Mockito.when(mockedArguments.getOptionNames()).thenReturn(Collections.emptySet());
        
        ConfigurableEnvironment mockedEnvironment = Mockito.mock(ConfigurableEnvironment.class);
        Mockito.when(mockedEnvironment.getProperty(JarvisConstants.CONTEXT)).thenReturn(contextPath);
        
        return new JarvisContextLoader(mockedEnvironment, mockedArguments);
    }
    
    @Test
    public void parseContextsJson() {
        
        JarvisContextLoader contextLoader = initContextLoader("src/test/resources/context/test-contexts.json");
        
        Map<String, JarvisContext> contextIdToContexts = contextLoader.getContextIdToContexts();
        assertEquals(6, contextIdToContexts.size());
        
        assertTrue(contextIdToContexts.containsValue(BQ_CONTEXT));
        assertTrue(contextIdToContexts.containsValue(LOCAL_CONTEXT));
        assertTrue(contextIdToContexts.containsValue(MSSQL_CONTEXT));
        assertTrue(contextIdToContexts.containsValue(MYSQL_CONTEXT));
        assertTrue(contextIdToContexts.containsValue(SFTP_CONTEXT));
        assertTrue(contextIdToContexts.containsValue(TALEND_API_CONTEXT));
    }
    
    @Test
    public void parseBQNoParamsContextJson() {
        exceptionRule.expect(IllegalStateException.class);
        initContextLoader("src/test/resources/context/bq-no-params-context.json");
    }
    
    @Test
    public void parseBQEmptyParamsContextJson() {
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Context(id=BQ, contextType=BigQuery, parameters={}) is missing parameters. Required parameters for this context type: project.");
        initContextLoader("src/test/resources/context/bq-empty-params-context.json");
    }
    
    @Test
    public void parseInvalidContextJson() {
        exceptionRule.expect(InvalidFormatException.class);
        initContextLoader("src/test/resources/context/invalid-context.json");
        
    }
}
