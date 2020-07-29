package ai.aliz.talendtestrunner.testconfig;

import lombok.Data;

import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ai.aliz.talendtestrunner.config.RetryConfig;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.service.TalendApiService;
import ai.aliz.talendtestrunner.util.PlaceholderResolver;

public abstract class ExecuteAction {
    
    
    public abstract void run(ContextLoader contextLoader);
    
    
    @Data
    @Service
    public static class TalendTask extends ExecuteAction {
        
        public String taskName;
    
        @Override
        public void run(ContextLoader contextLoader) {
            RetryConfig retryConfig = new RetryConfig();
            RetryTemplate retryTemplate = retryConfig.retryTemplate(retryConfig.retryPolicy(), retryConfig.backOffPolicy());
            TalendApiService talendApiService = new TalendApiService(new PlaceholderResolver(), new RestTemplate(), retryTemplate);
            talendApiService.executeTask(taskName, contextLoader.getContext("TalendAPI"));
        }
    }
}
