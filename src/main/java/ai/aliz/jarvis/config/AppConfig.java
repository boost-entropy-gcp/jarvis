package ai.aliz.jarvis.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
@Data
public class AppConfig {
    
    private String baseTestFolder;
    
    private boolean testFilteringEnabled;
    
    private boolean manualJobRun;
    
}

