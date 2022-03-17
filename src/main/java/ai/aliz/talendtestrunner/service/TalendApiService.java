package ai.aliz.talendtestrunner.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.talendtestrunner.talend.Executable;
import ai.aliz.talendtestrunner.talend.Execution;
import ai.aliz.talendtestrunner.talend.ExecutionStillRunningException;
import ai.aliz.jarvis.util.PlaceholderResolver;

@Service
@Slf4j
@AllArgsConstructor
public class TalendApiService {

    private static final String TASK_EXECUTE_ENDPOINT = "/executions";
    private static final String TASK_QUERY_PATTERN = "/executables?_s=" +
            "name=={{jobName}};" +
            "workspace.name=={{workspace}};" +
            "workspace.environment.name=={{environment}}";

    private static final String API_URL = "apiUrl";
    private static final String API_KEY = "apiKey";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private final PlaceholderResolver placeholderResolver;
    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;


    public void executeTask(String taskName, JarvisContext context) {
        Executable task = getTask(taskName, context);
        String executionId = runTask(task, context);
        waitForTaskToFinish(executionId, context);
    }

    private Executable getTask(String taskName, JarvisContext context) {
        Map<String, String> parameters = context.getParameters();
        parameters.put("jobName", taskName);

        String apiUrl = parameters.get(API_URL);
        String query = placeholderResolver.resolve(TASK_QUERY_PATTERN, parameters);
        String endpointUrl = apiUrl + query;

        RequestEntity<Void> requestEntity = RequestEntity.get(URI.create(endpointUrl))
                .header(AUTHORIZATION, BEARER + parameters.get(API_KEY))
                .build();

        ResponseEntity<List<Executable>> exchange = restTemplate.exchange(endpointUrl,
                HttpMethod.GET, requestEntity,
                new ParameterizedTypeReference<List<Executable>>() {
                });

        List<Executable> tasks = exchange.getBody();

        if (tasks.isEmpty()) {
            throw new RuntimeException("Task not found: " + taskName);
        }

        if (tasks.size() > 1) {
            throw new RuntimeException("Too many candidates for task: " + taskName);
        }
        Executable executable = tasks.get(0);
        log.info("Task for taskName {} is {}", taskName, executable);
        return executable;
    }

    private String runTask(Executable task, JarvisContext context) {
        String apiUrl = context.getParameters().get(API_URL);
        String endpointUrl = apiUrl + TASK_EXECUTE_ENDPOINT;
        RequestEntity<Map<String, String>> request = RequestEntity.post(URI.create(endpointUrl))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + context.getParameters().get(API_KEY))
                .body(ImmutableMap.of("executable", task.getExecutable()));

        Execution execution = restTemplate.exchange(request, Execution.class).getBody();
        String executionId = execution.getExecutionId();
        log.info("Execution id for task {} is {}", task, executionId);
        return executionId;
    }

    private void waitForTaskToFinish(String executionId, JarvisContext context) {
        Map<String, String> parameters = context.getParameters();

        String apiUrl = parameters.get(API_URL);
        String endpointUrl = apiUrl + TASK_EXECUTE_ENDPOINT + "/" + executionId;

        RequestEntity<Void> request =
                RequestEntity.get(URI.create(endpointUrl))
                        .accept(MediaType.APPLICATION_JSON)
                        .header(AUTHORIZATION, BEARER + parameters.get(API_KEY))
                        .build();


        retryTemplate.execute((RetryCallback<Void, ExecutionStillRunningException>) retryContext -> {
            Execution execution = restTemplate.exchange(request, Execution.class).getBody();
            String executionStatus = execution.getExecutionStatus();
            log.info("State of execution {} is {}", executionId, executionStatus);
            if (Execution.RUNNING_STATES.contains(executionStatus)) {
                throw new ExecutionStillRunningException();
            }

            if (Execution.SUCCESS_STATE.equals(executionStatus)) {
                return null;
            }

            throw new RuntimeException(String.format("Execution %s is in illegal state: %s ", executionId, executionStatus));
        });
    }
}
