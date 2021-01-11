package ai.aliz.jarvis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import ai.aliz.talendtestrunner.config.AppConfig;

@SpringBootApplication
public class JarvisApplication {
    
    public static void main(String[] args) {
        CommandLinePropertySource clps = new SimpleCommandLinePropertySource(args);
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(clps);
        ctx.register(AppConfig.class);
        ctx.refresh();
        SpringApplication.run(JarvisApplication.class, args);
    }
    
}
