package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.ContextType;
import ai.aliz.talendtestrunner.service.executor.AirflowExecutor;
import ai.aliz.talendtestrunner.service.executor.BqQueryExecutor;
import ai.aliz.talendtestrunner.service.executor.Executor;
import ai.aliz.talendtestrunner.service.executor.NoOpsExecutor;
import ai.aliz.talendtestrunner.service.executor.TalendExecutor;
import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import ai.aliz.talendtestrunner.config.AppConfig;
import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import ai.aliz.talendtestrunner.factory.TestStepFactory;
import ai.aliz.talendtestrunner.testcase.TestCase;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.testconfig.ExecutionType;
import ai.aliz.talendtestrunner.util.TestCollector;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;

@Service
@AllArgsConstructor
@Slf4j
public class TestRunnerService {
    
    private final ContextLoader contextLoader;
    private final TalendApiService talendApiService;
    private final TestStepFactory testStepFactory;
    private final ExecutorServiceImpl executorService;
    private final AppConfig config;
    private final ApplicationContext applicationContext;
    private final InitActionService initActionService;
    private final AssertActionService assertActionService;
    private final TalendJobStateChecker talendJobStateChecker;
    private final BigQueryExecutor bigQueryExecutor;
    private final ExecutionActionService executionActionService;
    
    private static Map<ExecutionType, Class<? extends Executor>> executorMap = new HashMap<>();
    
    static {
        executorMap.put(ExecutionType.BqQuery, BqQueryExecutor.class);
        executorMap.put(ExecutionType.Airflow, AirflowExecutor.class);
        executorMap.put(ExecutionType.Talend, TalendExecutor.class);
        executorMap.put(ExecutionType.NoOps, NoOpsExecutor.class);
    }
    
    
    public void runTest(ai.aliz.talendtestrunner.testconfig.TestCase testCase) {
        initActionService.run(testCase.getInitActionConfigs(), contextLoader);

        testCase.getExecutionActionConfigs().forEach(executionActionConfig -> {
    
            Class<? extends Executor> executorClass = executorMap.get(executionActionConfig.getType());
            Executor executor = applicationContext.getBean(executorClass);
            executor.execute(executionActionConfig);
        });

        testCase.getAssertActionConfigs().forEach(assertAction -> assertActionService.assertResult(assertAction, contextLoader));
    }
    
    @SneakyThrows
    public void runTest(TestCase testCase) {
        prepareTest(testCase);
        
        log.info("Test preparation successful for testCase {}", testCase);
        
        Context talendDatabaseContext = contextLoader.getContext("TalendDatabase");
        
        if (config.isManualJobRun()) {
            
            
            String jobState = talendJobStateChecker.getJobState(testCase.getJobName(), talendDatabaseContext);
            
            while (jobState.equals(talendJobStateChecker.getJobState(testCase.getJobName(), talendDatabaseContext))) {
                Thread.sleep(5000l);
                log.info("Waiting for execution on manual job run for testCase {}", testCase);
            }
        } else {
            log.info("Executing job for testcase {}", testCase);
            executeJob(testCase);
        }
        
        doAssertions(testCase);
    }
    
    private void doAssertions(TestCase testCase) {
        log.info("Doing assertions for test case {}", testCase);
        
        List<TestCollector.AssertionDefition> assertionDefinitions = testCase.getAssertionDefinitions();
        List<Runnable> assertionRunnables = assertionDefinitions.stream()
                                                                .map(testStepFactory::createAssertionRunnable)
                                                                .filter(runnable -> runnable != null)
                                                                .collect(Collectors.toList());
        
        executorService.executeRunnablesInParallel(assertionRunnables, 50, TimeUnit.SECONDS);
    }
    
    private void executeJob(TestCase testCase) {
        log.info("Executing job for test case {}", testCase);
        Context remoteEngineContext = contextLoader.getContext("RemoteEngine");
        
        talendApiService.executeTask(testCase.getJobName(), remoteEngineContext);
    }
    
    private void prepareTest(TestCase testCase) throws InterruptedException, java.util.concurrent.ExecutionException {
        log.info("Preparing test {}", testCase);
        List<Path> preparationFiles = testCase.getPreparationFiles();
        List<Runnable> prepareCallables = preparationFiles.stream().map(testStepFactory::createPreparationRunnable)
                                                          .collect(Collectors.toList());
        executorService.executeRunnablesInParallel(prepareCallables, 150, TimeUnit.SECONDS);
    }
    
    @SneakyThrows
    private void runTalendJob(ContextLoader contextLoader, ExecutionActionConfig executionActionConfig, ai.aliz.talendtestrunner.testconfig.TestCase testCase) {
        Context talendDatabaseContext = contextLoader.getContext("TalendDatabase");

        String taskName = executionActionConfig.getProperties().get("sourcePath").toString();

        if (config.isManualJobRun()) {



            Optional<AssertActionConfig> talendStateAssertActionConfig = testCase.getAssertActionConfigs()
                                                                                 .stream()
                                                                                 .filter(a -> contextLoader.getContext(a.getSystem()).getContextType() == ContextType.MySQL)
                                                                                 .findAny();

            if(talendStateAssertActionConfig.isPresent()) {
                String jobState = talendJobStateChecker.getJobState(taskName, talendDatabaseContext);

                while (jobState.equals(talendJobStateChecker.getJobState(taskName, talendDatabaseContext))) {
                    Thread.sleep(5000l);
                    log.info("Waiting for execution on manual job run for testCase {}", executionActionConfig);
                }
            } else {
                AssertActionConfig bqTableAssertActionConfig = testCase.getAssertActionConfigs()
                                                                       .stream()
                                                                       .filter(a -> "AssertDataEquals".equals(a.getType()) && contextLoader.getContext(a.getSystem()).getContextType() == ContextType.BigQuery)
                                                                       .findAny()
                                                                       .get();

                Map<String, Object> properties = bqTableAssertActionConfig.getProperties();

                Context context = contextLoader.getContext(bqTableAssertActionConfig.getSystem());
                String dataset = TestRunnerUtil.getDatasetName(properties, context);
                String table =(String) properties.get("table");
                String project = context.getParameters().get("project");
                Long lastModifiedAt = bigQueryExecutor.getTableLastModifiedAt(context, project, dataset, table);

                while (lastModifiedAt.equals(bigQueryExecutor.getTableLastModifiedAt(context, project, dataset, table))) {
                    Thread.sleep(5000l);
                    log.info("Waiting for execution on manual job run for testCase {}", executionActionConfig);
                }
            }
        } else {
            log.info("Executing job for testcase {}", executionActionConfig);
            executionActionService.run(contextLoader, taskName);
        }

    }
}
