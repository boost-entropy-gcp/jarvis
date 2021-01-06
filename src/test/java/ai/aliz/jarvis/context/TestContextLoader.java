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
    
    @Autowired
    private ContextLoader contextLoader;
    
    @Test
    public void parseBQContextJson() {
        contextLoader.parseContext("src/test/resources/context/bq-context.json");
        Set<Context> contexts = contextLoader.getContexts();
        assertThat(contexts, IsCollectionWithSize.hasSize(1));
        
        Context bqContext = Context.builder()
                                   .id("BQ")
                                   .contextType(ContextType.BigQuery)
                                   .parameter("project", "nora-ambroz-sandbox")
                                   .parameter("datasetNamePrefix", "core_")
                                   .parameter("stagingDataset", "STAGING")
                                   .build();
        
        assertTrue(contexts.contains(bqContext));
    }
    
    //TODO throw a meaningful exception with an appropriate error message, when a required parameter is missing
    @Test(expected = JsonMappingException.class)
    public void parseBQNoParamsContextJson() {
        contextLoader.parseContext("src/test/resources/context/bq-no-params-context.json");
    }
    
    @Test
    public void parseLocalContextJson() {
        contextLoader.parseContext("src/test/resources/context/local-context.json");
        Set<Context> contexts = contextLoader.getContexts();
        assertThat(contexts, IsCollectionWithSize.hasSize(1));
        
        Context localContext = Context.builder()
                                      .id("local")
                                      .contextType(ContextType.LocalContext)
                                      .parameter("repositoryRoot", "C:\\ds")
                                      .build();
        
        assertTrue(contexts.contains(localContext));
    }
    
    @Test
    public void parseMySQLContextJson() {
        contextLoader.parseContext("src/test/resources/context/my-sql-context.json");
        Set<Context> contexts = contextLoader.getContexts();
        assertThat(contexts, IsCollectionWithSize.hasSize(1));
        
        Context mysqlContext = Context.builder()
                                      .id("MySQL")
                                      .contextType(ContextType.MySQL)
                                      .parameter("host", "host")
                                      .parameter("port", "port")
                                      .parameter("database", "database")
                                      .parameter("user", "user")
                                      .parameter("password", "password")
                                      .build();
        
        assertTrue(contexts.contains(mysqlContext));
    }
    
    @Test
    public void parseMSSQLContextJson() {
        contextLoader.parseContext("src/test/resources/context/mssql-context.json");
        Set<Context> contexts = contextLoader.getContexts();
        assertThat(contexts, IsCollectionWithSize.hasSize(1));
        
        Context mssqlContext = Context.builder()
                                      .id("MSSQL")
                                      .contextType(ContextType.MSSQL)
                                      .parameter("host", "host")
                                      .parameter("port", "port")
                                      .parameter("database", "database")
                                      .parameter("user", "user")
                                      .parameter("password", "password")
                                      .build();
        
        assertTrue(contexts.contains(mssqlContext));
    }
    
    @Test
    public void parseSFTPContextJson() {
        contextLoader.parseContext("src/test/resources/context/sftp-context.json");
        Set<Context> contexts = contextLoader.getContexts();
        assertThat(contexts, IsCollectionWithSize.hasSize(1));
        
        Context sftpContext = Context.builder()
                                     .id("SFTP")
                                     .contextType(ContextType.SFTP)
                                     .parameter("host", "host")
                                     .parameter("port", "port")
                                     .parameter("user", "user")
                                     .parameter("password", "password")
                                     .parameter("remoteBasePath", "/out")
                                     .build();
        
        assertTrue(contexts.contains(sftpContext));
    }
    
    @Test
    public void parseTalendAPIContextJson() {
        contextLoader.parseContext("src/test/resources/context/talend-api-context.json");
        Set<Context> contexts = contextLoader.getContexts();
        assertThat(contexts, IsCollectionWithSize.hasSize(1));
        
        Context talendAPIContext = Context.builder()
                                          .id("TalendAPI")
                                          .contextType(ContextType.TalendAPI)
                                          .parameter("apiUrl", "url")
                                          .parameter("apiKey", "key")
                                          .parameter("environment", "environment")
                                          .parameter("workspace", "workspace")
                                          .build();
        
        assertTrue(contexts.contains(talendAPIContext));
    }
    
    @Test
    public void parseMultipleContextJson() {
        contextLoader.parseContext("src/test/resources/context/multiple-context.json");
        Set<Context> contexts = contextLoader.getContexts();
        assertThat(contexts, IsCollectionWithSize.hasSize(5));
    }
    
    //TODO throw a meaningful exception with a helpful error message, when the context type is invalid
    @Test(expected = InvalidFormatException.class)
    public void parseInvalidContextJson() {
        contextLoader.parseContext("src/test/resources/context/invalid-context.json");
    }
}
