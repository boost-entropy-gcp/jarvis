package ai.aliz.talendtestrunner.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.context.TestContextType;
import ai.aliz.jarvis.testconfig.AssertActionConfig;
import ai.aliz.jarvis.testconfig.ExecutionActionConfig;
import ai.aliz.jarvis.testconfig.ExecutionType;
import ai.aliz.jarvis.testconfig.TestCase;

import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import ai.aliz.talendtestrunner.service.executor.AirflowExecutor;
import ai.aliz.talendtestrunner.service.executor.BqScriptExecutor;
import ai.aliz.talendtestrunner.service.executor.Executor;
import ai.aliz.talendtestrunner.service.executor.NoOpsExecutor;
import ai.aliz.talendtestrunner.service.executor.TalendExecutor;

import ai.aliz.talendtestrunner.util.TestRunnerUtil;

import static ai.aliz.talendtestrunner.helper.Helper.PROJECT;
import static ai.aliz.talendtestrunner.helper.Helper.SOURCE_PATH;
import static ai.aliz.talendtestrunner.helper.Helper.TABLE;

@Service
@AllArgsConstructor
@Slf4j
public class TestRunnerService {
    
    private final TestContextLoader contextLoader;
    private final TalendApiService talendApiService;
    private final ExecutorServiceImpl executorService;
    private final ApplicationContext applicationContext;
    private final AssertActionService assertActionService;
    private final TalendJobStateChecker talendJobStateChecker;
    private final BigQueryExecutor bigQueryExecutor;
    private final ExecutionActionService executionActionService;
    
    private static Map<ExecutionType, Class<? extends Executor>> executorMap = new HashMap<>();
    
    static {
        executorMap.put(ExecutionType.BqQuery, BqScriptExecutor.class);
        executorMap.put(ExecutionType.Airflow, AirflowExecutor.class);
        executorMap.put(ExecutionType.Talend, TalendExecutor.class);
        executorMap.put(ExecutionType.NoOps, NoOpsExecutor.class);
    }
    
    
    public void runTest(TestCase testCase) {

        testCase.getExecutionActionConfigs().forEach(executionActionConfig -> {
    
            Class<? extends Executor> executorClass = Objects.requireNonNull(executorMap.get(executionActionConfig.getType()));
            Executor executor = applicationContext.getBean(executorClass);
            executor.execute(executionActionConfig);
        });

        testCase.getAssertActionConfigs().forEach(assertAction -> assertActionService.assertResult(assertAction, contextLoader));
    }
    
  
    @SneakyThrows
    public void runTalendJob(TestContextLoader contextLoader, ExecutionActionConfig executionActionConfig) {
        TestContext talendDatabaseContext = contextLoader.getContext("TalendDatabase");

        String taskName = executionActionConfig.getProperties().get(SOURCE_PATH).toString();

        if (true) {



            Optional<AssertActionConfig> talendStateAssertActionConfig = executionActionConfig.getAssertActionConfigs()
                                                                                 .stream()
                                                                                 .filter(a -> contextLoader.getContext(a.getSystem()).getContextType() == TestContextType.MySQL)
                                                                                 .findAny();

            if(talendStateAssertActionConfig.isPresent()) {
                String jobState = talendJobStateChecker.getJobState(taskName, talendDatabaseContext);

                while (jobState.equals(talendJobStateChecker.getJobState(taskName, talendDatabaseContext))) {
                    Thread.sleep(5000l);
                    log.info("Waiting for execution on manual job run for testCase {}", executionActionConfig);
                }
            } else {
                AssertActionConfig bqTableAssertActionConfig = executionActionConfig.getAssertActionConfigs()
                                                                       .stream()
                                                                       .filter(a -> "AssertDataEquals".equals(a.getType()) && contextLoader.getContext(a.getSystem()).getContextType() == TestContextType.BigQuery)
                                                                       .findAny()
                                                                       .get();

                Map<String, Object> properties = bqTableAssertActionConfig.getProperties();

                TestContext context = contextLoader.getContext(bqTableAssertActionConfig.getSystem());
                String dataset = TestRunnerUtil.getDatasetName(properties, context);
                String table =(String) properties.get(TABLE);
                String project = context.getParameters().get(PROJECT);
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
