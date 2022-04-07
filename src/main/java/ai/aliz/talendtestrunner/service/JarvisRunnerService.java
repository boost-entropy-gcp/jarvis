package ai.aliz.talendtestrunner.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ai.aliz.jarvis.context.JarvisContextLoader;
import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.config.ExecutionActionConfig;
import ai.aliz.jarvis.config.JarvisTestCase;

import static ai.aliz.jarvis.util.Helper.SOURCE_PATH;

@Service
@AllArgsConstructor
@Slf4j
public class JarvisRunnerService {
    
    private final JarvisContextLoader contextLoader;
    private final TalendApiService talendApiService;
    private final ApplicationContext applicationContext;
    private final TalendJobStateChecker talendJobStateChecker;
    private final ExecutionActionService executionActionService;
    
    
   
    
    
    public void runJarvis(JarvisTestCase jarvisTestCase) {

        jarvisTestCase.getExecutionActionConfigs().forEach(executionActionConfig -> {
    
//            Class<? extends Executor> executorClass = Objects.requireNonNull(executorMap.get(executionActionConfig.getType()));
//            Executor executor = applicationContext.getBean(executorClass);
//            executor.execute(executionActionConfig);
        });
        
    }
    
  
    @SneakyThrows
    public void runTalendJob(JarvisContextLoader contextLoader, ExecutionActionConfig executionActionConfig) {
        JarvisContext talendDatabaseContext = contextLoader.getContext("TalendDatabase");

        String taskName = executionActionConfig.getProperties().get(SOURCE_PATH).toString();

        if (true) {



//            Optional<AssertActionConfig> talendStateAssertActionConfig = executionActionConfig.getAssertActionConfigs()
//                                                                                 .stream()
//                                                                                 .filter(a -> contextLoader.getContext(a.getSystem()).getContextType() == TestContextType.MySQL)
//                                                                                 .findAny();
//
//            if(talendStateAssertActionConfig.isPresent()) {
//                String jobState = talendJobStateChecker.getJobState(taskName, talendDatabaseContext);
//
//                while (jobState.equals(talendJobStateChecker.getJobState(taskName, talendDatabaseContext))) {
//                    Thread.sleep(5000l);
//                    log.info("Waiting for execution on manual job run for testCase {}", executionActionConfig);
//                }
//            } else {
//                AssertActionConfig bqTableAssertActionConfig = executionActionConfig.getAssertActionConfigs()
//                                                                       .stream()
//                                                                       .filter(a -> "AssertDataEquals".equals(a.getType()) && contextLoader.getContext(a.getSystem()).getContextType() == TestContextType.BigQuery)
//                                                                       .findAny()
//                                                                       .get();
//
//                Map<String, Object> properties = bqTableAssertActionConfig.getProperties();
//
//                TestContext context = contextLoader.getContext(bqTableAssertActionConfig.getSystem());
//                String dataset = TestRunnerUtil.getDatasetName(properties, context);
//                String table =(String) properties.get(TABLE);
//                String project = context.getParameters().get(PROJECT);
//                Long lastModifiedAt = bigQueryExecutor.getTableLastModifiedAt(context, project, dataset, table);
//
//                while (lastModifiedAt.equals(bigQueryExecutor.getTableLastModifiedAt(context, project, dataset, table))) {
//                    Thread.sleep(5000l);
//                    log.info("Waiting for execution on manual job run for testCase {}", executionActionConfig);
//                }
//            }
        } else {
            log.info("Executing job for jarvisTestcase {}", executionActionConfig);
            executionActionService.run(contextLoader, taskName);
        }

    }
}
