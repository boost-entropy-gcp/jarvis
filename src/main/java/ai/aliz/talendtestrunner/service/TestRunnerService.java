package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.testconfig.ExecutionActionConfig;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ai.aliz.talendtestrunner.config.AppConfig;
import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.context.Type;
import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import ai.aliz.talendtestrunner.factory.TestStepFactory;
import ai.aliz.talendtestrunner.testcase.TestCase;
import ai.aliz.talendtestrunner.testconfig.AssertActionConfig;
import ai.aliz.talendtestrunner.testconfig.TalendTask;
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
    private final InitActionService initActionService;
    private final AssertActionService assertActionService;
    private final TalendJobStateChecker talendJobStateChecker;
    private final BigQueryExecutor bigQueryExecutor;
    private final ExecutionActionService executionActionService;
    
    public void runTest(ai.aliz.talendtestrunner.testconfig.TestCase testCase) {
        initActionService.run(testCase.getInitActionConfigs(), contextLoader);

        testCase.getExecutionActionConfigs().forEach(executionActionConfig -> {
            switch (executionActionConfig.getType()) {
                case "airFlow" :
                case "BqQuery" :
                    bq();
                case "noOps" :
                case "talend":
                    runTalendJob(contextLoader, executionActionConfig, testCase);
            }
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

        TalendTask talendTask = new TalendTask(executionActionConfig.getType());

        if (config.isManualJobRun()) {



            Optional<AssertActionConfig> talendStateAssertActionConfig = testCase.getAssertActionConfigs()
                                                                                 .stream()
                                                                                 .filter(a -> contextLoader.getContext(a.getSystem()).getType() == Type.MySQL)
                                                                                 .findAny();

            if(talendStateAssertActionConfig.isPresent()) {
                String jobState = talendJobStateChecker.getJobState(talendTask.getTaskName(), talendDatabaseContext);

                while (jobState.equals(talendJobStateChecker.getJobState(talendTask.getTaskName(), talendDatabaseContext))) {
                    Thread.sleep(5000l);
                    log.info("Waiting for execution on manual job run for testCase {}", executionActionConfig);
                }
            } else {
                AssertActionConfig bqTableAssertActionConfig = testCase.getAssertActionConfigs()
                                                                       .stream()
                                                                       .filter(a -> "AssertDataEquals".equals(a.getType()) && contextLoader.getContext(a.getSystem()).getType() == Type.BigQuery)
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
            executionActionService.run(contextLoader, talendTask);
        }

    }

    private void bq() {
        System.out.println("bqType");
    }
    
}
