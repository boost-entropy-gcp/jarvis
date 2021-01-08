package ai.aliz.jarvis.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ContextLoaderConfig {
    
    @Autowired
    Environment env;
    
    @Bean
    public ContextLoader contextLoader() {
        return new ContextLoader(env.getProperty("context"));
    }
}
