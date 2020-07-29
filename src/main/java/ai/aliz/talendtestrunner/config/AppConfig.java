package ai.aliz.talendtestrunner.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
@Getter
@Setter
public class AppConfig {

    private String baseTestFolder;

    private boolean testFilteringEnabled;

    private boolean manualJobRun;

}
