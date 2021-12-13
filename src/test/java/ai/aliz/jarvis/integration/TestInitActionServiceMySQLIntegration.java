package ai.aliz.jarvis.integration;

import lombok.SneakyThrows;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Preconditions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.testconfig.InitActionConfigFactory;
import ai.aliz.jarvis.service.initaction.InitActionService;
import ai.aliz.jarvis.testconfig.InitActionConfig;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = "context=src/test/resources/integration/mysql-context.json")
public class TestInitActionServiceMySQLIntegration {
    
     /*
    PREREQUISITES
     * The test requires an existing GCP project with Cloud SQL Admin API enabled.
     * To provide resources for this test, apply the Terraform configurations in the integration folder.
    */
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    @Autowired
    private InitActionConfigFactory initActionConfigFactory;
    private static Connection CONNECTION;
    
    @Autowired
    private InitActionService actionService;
    @Autowired
    private TestContextLoader contextLoader;
    
    @BeforeClass
    @SneakyThrows
    public static void init() {
//        Map<String, String> mysqlParameters = new ContextLoader("src/test/resources/integration/mysql-context.json").getContext("MySQL").getParameters();
//        CONNECTION = DriverManager.getConnection("jdbc:mysql://" + mysqlParameters.get(HOST) +
//                                                         ":" + mysqlParameters.get(PORT) +
//                                                         "/" + mysqlParameters.get(DATABASE) +
//                                                         "?user=" + mysqlParameters.get(USER) +
//                                                         "&password=" + mysqlParameters.get(PASSWORD));
    }
    
    @Test
    @SneakyThrows
    public void testWithScript() {
        Statement statement = CONNECTION.createStatement();
        String validationQuery = "SELECT * FROM test";
        ResultSet firstState = statement.executeQuery(validationQuery);
        Preconditions.checkArgument(!firstState.isBeforeFirst());
        
        try {
            List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/integration/mysql-script"));
            actionService.run(actionConfigs);
            
            ResultSet resultSet = statement.executeQuery(validationQuery);
            assertTrue(resultSet.next());
            assertEquals(12, resultSet.getInt("test_id"));
            assertEquals("dummy name", resultSet.getString("test_name"));
        } finally {
            statement.execute("DELETE FROM test");
        }
    }
    
    @Test
    @SneakyThrows
    public void testInvalidScript() {
        exceptionRule.expect(ExecutionException.class);
        exceptionRule.expectMessage("Table 'JarvisMySQLDB.invalid' doesn't exist");
        
        List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/integration/mysql-script-invalid"));
        actionService.run(actionConfigs);
    }
}
