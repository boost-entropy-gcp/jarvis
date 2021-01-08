package ai.aliz.jarvis;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import ai.aliz.jarvis.context.ContextLoader;
import ai.aliz.talendtestrunner.config.AppConfig;

@SpringBootApplication
@Log4j2
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
