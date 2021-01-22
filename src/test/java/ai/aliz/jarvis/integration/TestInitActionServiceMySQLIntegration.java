package ai.aliz.jarvis.integration;

import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DriverManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ai.aliz.jarvis.context.ContextLoader;
import ai.aliz.jarvis.service.init.InitActionConfigFactory;
import ai.aliz.jarvis.service.init.InitActionService;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = "context=src/test/resources/integration/integration-contexts.json")
public class TestInitActionServiceMySQLIntegration {
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    private final InitActionConfigFactory initActionConfigFactory = new InitActionConfigFactory();
    private static Connection CONNECTION;
    
    @Autowired
    private InitActionService actionService;
    @Autowired
    private ContextLoader contextLoader;
    
    @BeforeClass
    @SneakyThrows
    public static void init() {
        CONNECTION = DriverManager.getConnection("jdbc:sqlserver://34.77.231.35:1433;databaseName=JarvisMSDB;user=admin;password=nG29k0IrcSCY");
    }
    
}
