package ai.aliz.talendtestrunner.config;

import ai.aliz.talendtestrunner.talend.ExecutionStillRunningException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import static java.util.Collections.singletonMap;

@Configuration
public class RetryConfig {

    private static final int MAX_ATTEMPTS = 100;
    private static final int BACK_OFF_PERIOD = 10000;

    @Bean
    public RetryPolicy retryPolicy() {
        return new SimpleRetryPolicy(MAX_ATTEMPTS, singletonMap(ExecutionStillRunningException.class, true));
    }

    @Bean
    public BackOffPolicy backOffPolicy() {
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(BACK_OFF_PERIOD);
        return fixedBackOffPolicy;
    }

    @Bean
    public RetryTemplate retryTemplate(RetryPolicy retryPolicy, BackOffPolicy backOffPolicy) {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}
