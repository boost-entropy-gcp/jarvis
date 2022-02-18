package ai.aliz.jarvis.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aliz.jarvis.service.assertaction.AssertActionService;
import ai.aliz.jarvis.service.executeaction.ExecuteActionService;
import ai.aliz.jarvis.service.initaction.InitActionService;
import ai.aliz.jarvis.testconfig.TestCase;
import ai.aliz.jarvis.testconfig.TestConfigLoader;
import ai.aliz.jarvis.testconfig.TestSuite;

@Component
@Slf4j
public class TestRunnerService {
    
    @Autowired
    private TestConfigLoader configLoader;
    
    @Autowired
    private InitActionService initActionService;
    
    @Autowired
    private ExecuteActionService executeActionService;
    
    @Autowired
    private AssertActionService assertActionService;
    
    
    public void runTestSuite(TestSuite testSuite) {
        testSuite.getTestCases().forEach(this::runTestCase);
        testSuite.getSuites().forEach(this::runTestSuite);
    }
    
    public void runTestCase(TestCase testCase) {
        log.info("Start executing testcase: {}", testCase);
        initActionService.run(testCase.getInitActionConfigs());
        executeActionService.run(testCase.getExecutionActionConfigs());
        assertActionService.run(testCase.getAssertActionConfigs());
        log.info("Testcase execution finished: {}", testCase);
    }
    
    
}
