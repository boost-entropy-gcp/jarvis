package ai.aliz.talendtestrunner;

import lombok.extern.log4j.Log4j2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@Log4j2
public class TalendTestRunnerApplication {
    
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TalendTestRunnerApplication.class, args);
    }
    
//    @Bean
//    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
//        return args -> {
//
//            Options options =
//                    new Options()
//                            .addOption(Option.builder("tc").longOpt("testConfigFile").hasArg().required().build())
//                            .addOption(Option.builder("c").longOpt("context").hasArg().required().build())
//                            .addOption(Option.builder("case").longOpt("case").hasArg().required(false).build());
//
//            CommandLineParser parser = new DefaultParser();
//            HelpFormatter formatter = new HelpFormatter();
//            CommandLine cmd = null;
//            try {
//                cmd = parser.parse(options, args);
//            } catch (ParseException e) {
//                System.out.println(e.getMessage());
//                formatter.printHelp("utility-name", options);
//
//                System.exit(1);
//            }
//
//            String testConfigPath = cmd.getOptionValue("tc");
//            String contextPath = cmd.getOptionValue("c");
//            String targetPrefix = cmd.getOptionValue("case");
//
//            final ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
//            contextLoader.parseContext(contextPath);
//
//            TestSuite testSuite = TestSuite.readTestConfig(testConfigPath, contextLoader);
//
//            executeTestSuite(testSuite, contextLoader, ctx, targetPrefix);
//
//            System.exit(0);
//
//        };
//    }
//
//    private void executeTestSuite(TestSuite testSuite, ContextLoader contextLoader, ApplicationContext ctx, String targetPrefix) {
//
//        InitActionService initActionService = ctx.getBean(InitActionService.class);
//        AssertActionService assertActionService = ctx.getBean(AssertActionService.class);
//
//        testSuite.getTestCases().stream().filter(testCase -> targetPrefix == null || targetPrefix.equals(testCase.getName())).forEach(testCase -> {
////            initActionService.run(testCase.getInitActionConfigs(), contextLoader);
////            testCase.getExecuteActions().forEach(executeAction -> runTalendJob(contextLoader, executeAction, ctx));
//            testCase.getAssertActionConfigs().forEach(assertAction -> assertActionService.assertResult(assertAction, contextLoader));
//        });
//
//        testSuite.getSuites().forEach(suite -> this.executeTestSuite(suite, contextLoader, ctx, targetPrefix));
//    }
//
//    @SneakyThrows
//    private void runTalendJob(ContextLoader contextLoader, ExecuteAction executeAction, ApplicationContext applicationContext) {
//        AppConfig config = applicationContext.getBean(AppConfig.class);
//        if (config.isManualJobRun()) {
//            log.info("Waiting for confirmation on manual job run for testCase {}", executeAction);
//            // need to start IDEA with -Deditable.java.test.console=true argument to be able to provide input while
//            // running tests. see https://stackoverflow.com/questions/38482844/reading-system-in-from-the-console-using-intellij-and-junit
//            new BufferedReader(new InputStreamReader(System.in)).readLine();
//        } else {
//            log.info("Executing job for testcase {}", executeAction);
//            executeAction.run(contextLoader);
//        }
//
//    }
    
}
