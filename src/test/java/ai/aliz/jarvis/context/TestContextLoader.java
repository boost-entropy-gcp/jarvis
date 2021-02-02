package ai.aliz.jarvis.context;

import java.util.Map;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

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

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ContextLoaderConfig.class)
@TestPropertySource(properties = "context=src/test/resources/context/test-contexts.json")
public class TestContextLoader {
    
    private static final Context BQ_CONTEXT = Context.builder()
                                                     .id("BQ")
                                                     .contextType(ContextType.BigQuery)
                                                     .parameter(PROJECT, "nora-ambroz-sandbox")
                                                     .parameter(DATASET_NAME_PREFIX, "core_")
                                                     .parameter("stagingDataset", "STAGING")
                                                     .build();
    
    private static final Context LOCAL_CONTEXT = Context.builder()
                                                        .id("local")
                                                        .contextType(ContextType.LocalContext)
                                                        .parameter(REPOSITORY_ROOT, "C:\\ds")
                                                        .build();
    
    private static final Context MSSQL_CONTEXT = Context.builder()
                                                        .id("MSSQL")
                                                        .contextType(ContextType.MSSQL)
                                                        .parameter(HOST, "host")
                                                        .parameter(PORT, "port")
                                                        .parameter(DATABASE, "database")
                                                        .parameter(USER, "user")
                                                        .parameter(PASSWORD, "password")
                                                        .build();
    
    private static final Context MYSQL_CONTEXT = Context.builder()
                                                        .id("MySQL")
                                                        .contextType(ContextType.MySQL)
                                                        .parameter(HOST, "host")
                                                        .parameter(PORT, "port")
                                                        .parameter(DATABASE, "database")
                                                        .parameter(USER, "user")
                                                        .parameter(PASSWORD, "password")
                                                        .build();
    
    private static final Context SFTP_CONTEXT = Context.builder()
                                                       .id("SFTP")
                                                       .contextType(ContextType.SFTP)
                                                       .parameter(HOST, "host")
                                                       .parameter(PORT, "port")
                                                       .parameter(USER, "user")
                                                       .parameter(PASSWORD, "password")
                                                       .parameter(REMOTE_BASE_PATH, "/out")
                                                       .build();
    
    private static final Context TALEND_API_CONTEXT = Context.builder()
                                                             .id("TalendAPI")
                                                             .contextType(ContextType.TalendAPI)
                                                             .parameter(API_URL, "url")
                                                             .parameter(API_KEY, "key")
                                                             .parameter(ENVIRONMENT, "environment")
                                                             .parameter(WORKSPACE, "workspace")
                                                             .build();
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    @Test
    public void parseContextsJson() {
        ContextLoader contextLoader = new ContextLoader("src/test/resources/context/test-contexts.json");
        Map<String, Context> contextIdToContexts = contextLoader.getContextIdToContexts();
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
        new ContextLoader("src/test/resources/context/bq-no-params-context.json");
    }
    
    @Test
    public void parseBQEmptyParamsContextJson() {
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage("Context(id=BQ, contextType=BigQuery, parameters={}) is missing parameters. Required parameters for this context type: project.");
        new ContextLoader("src/test/resources/context/bq-empty-params-context.json");
    }
    
    @Test
    public void parseInvalidContextJson() {
        exceptionRule.expect(InvalidFormatException.class);
        new ContextLoader("src/test/resources/context/invalid-context.json");
    }
}
