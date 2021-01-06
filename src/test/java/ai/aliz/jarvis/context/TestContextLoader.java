package ai.aliz.jarvis.context;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import org.hamcrest.collection.IsCollectionWithSize;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestContextLoader {
    
    private static final Context BQ_CONTEXT = Context.builder()
                                                     .id("BQ")
                                                     .contextType(ContextType.BigQuery)
                                                     .parameter("project", "nora-ambroz-sandbox")
                                                     .parameter("datasetNamePrefix", "core_")
                                                     .parameter("stagingDataset", "STAGING")
                                                     .build();
    
    private static final Context LOCAL_CONTEXT = Context.builder()
                                                        .id("local")
                                                        .contextType(ContextType.LocalContext)
                                                        .parameter("repositoryRoot", "C:\\ds")
                                                        .build();
    
    private static final Context MSSQL_CONTEXT = Context.builder()
                                                        .id("MSSQL")
                                                        .contextType(ContextType.MSSQL)
                                                        .parameter("host", "host")
                                                        .parameter("port", "port")
                                                        .parameter("database", "database")
                                                        .parameter("user", "user")
                                                        .parameter("password", "password")
                                                        .build();
    
    private static final Context MYSQL_CONTEXT = Context.builder()
                                                        .id("MySQL")
                                                        .contextType(ContextType.MySQL)
                                                        .parameter("host", "host")
                                                        .parameter("port", "port")
                                                        .parameter("database", "database")
                                                        .parameter("user", "user")
                                                        .parameter("password", "password")
                                                        .build();
    
    private static final Context SFTP_CONTEXT = Context.builder()
                                                       .id("SFTP")
                                                       .contextType(ContextType.SFTP)
                                                       .parameter("host", "host")
                                                       .parameter("port", "port")
                                                       .parameter("user", "user")
                                                       .parameter("password", "password")
                                                       .parameter("remoteBasePath", "/out")
                                                       .build();
    
    private static final Context TALEND_API_CONTEXT = Context.builder()
                                                             .id("TalendAPI")
                                                             .contextType(ContextType.TalendAPI)
                                                             .parameter("apiUrl", "url")
                                                             .parameter("apiKey", "key")
                                                             .parameter("environment", "environment")
                                                             .parameter("workspace", "workspace")
                                                             .build();
    
    @Autowired
    private ContextLoader contextLoader;
    
    @Test
    public void parseContextsJson() {
        contextLoader.parseContext("src/test/resources/context/test-contexts.json");
        Set<Context> contexts = contextLoader.getContexts();
        assertThat(contexts, IsCollectionWithSize.hasSize(6));
        
        assertTrue(contexts.contains(BQ_CONTEXT));
        assertTrue(contexts.contains(LOCAL_CONTEXT));
        assertTrue(contexts.contains(MSSQL_CONTEXT));
        assertTrue(contexts.contains(MYSQL_CONTEXT));
        assertTrue(contexts.contains(SFTP_CONTEXT));
        assertTrue(contexts.contains(TALEND_API_CONTEXT));
    }
    
    //TODO throw a meaningful exception with an appropriate error message, when a required parameter is missing
    @Test(expected = JsonMappingException.class)
    public void parseBQNoParamsContextJson() {
        contextLoader.parseContext("src/test/resources/context/bq-no-params-context.json");
    }
    
    //TODO throw a meaningful exception with a helpful error message, when the context type is invalid
    @Test(expected = InvalidFormatException.class)
    public void parseInvalidContextJson() {
        contextLoader.parseContext("src/test/resources/context/invalid-context.json");
    }
}
