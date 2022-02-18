package ai.aliz.jarvis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import ai.aliz.jarvis.service.TestRunnerService;
import ai.aliz.jarvis.testconfig.TestConfigLoader;

@SpringBootApplication
public class JarvisApplication implements CommandLineRunner {
    
    @Autowired
    private TestConfigLoader configLoader;
    
    @Autowired
    private TestRunnerService testRunnerService;
    
    public static void main(String[] args) {
        CommandLinePropertySource commandLinePropertySource = new SimpleCommandLinePropertySource(args);
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(commandLinePropertySource);
        ctx.refresh();
        SpringApplication.run(JarvisApplication.class, args);
        SpringApplication.exit(ctx);
        ctx.close();
    }
    
    @Override
    public void run(String... args) throws Exception {
        testRunnerService.runTestSuite(configLoader.getTestSuite());
    }
    
}
