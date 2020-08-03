package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.config.RetryConfig;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.testconfig.TalendTask;
import ai.aliz.talendtestrunner.util.PlaceholderResolver;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ExecutionActionService {


    public void run(ContextLoader contextLoader, TalendTask talendTask) {
        RetryConfig retryConfig = new RetryConfig();
        RetryTemplate retryTemplate = retryConfig.retryTemplate(retryConfig.retryPolicy(), retryConfig.backOffPolicy());
        TalendApiService talendApiService = new TalendApiService(new PlaceholderResolver(), new RestTemplate(), retryTemplate);
        talendApiService.executeTask(talendTask.getTaskName(), contextLoader.getContext("TalendAPI"));
    }
}
