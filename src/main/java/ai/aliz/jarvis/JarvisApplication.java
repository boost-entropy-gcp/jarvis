package ai.aliz.jarvis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import ai.aliz.jarvis.config.ConfigLoader;
import ai.aliz.jarvis.service.JarvisRunnerService;

@SpringBootApplication
public class JarvisApplication implements CommandLineRunner {
    
    @Autowired
    private ConfigLoader configLoader;
    
    @Autowired
    private JarvisRunnerService jarvisRunnerService;
    
    @SuppressWarnings("resource")
    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(JarvisApplication.class, args));
    }
    
    @Override
    public void run(String... args) throws Exception {
        jarvisRunnerService.runJarvisTestSuite(configLoader.getJarvisTestSuite());
    }
    
}
