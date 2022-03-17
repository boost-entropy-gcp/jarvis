package ai.aliz.jarvis.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ai.aliz.jarvis.config.JarvisTestSuite;
import ai.aliz.jarvis.service.assertaction.AssertActionService;
import ai.aliz.jarvis.service.executeaction.ExecuteActionService;
import ai.aliz.jarvis.service.initaction.InitActionService;
import ai.aliz.jarvis.config.JarvisTestCase;
import ai.aliz.jarvis.config.ConfigLoader;

@Component
@Slf4j
public class JarvisRunnerService {
    
    @Autowired
    private ConfigLoader configLoader;
    
    @Autowired
    private InitActionService initActionService;
    
    @Autowired
    private ExecuteActionService executeActionService;
    
    @Autowired
    private AssertActionService assertActionService;
    

    public void runJarvisTestSuite(JarvisTestSuite jarvisTestSuite) {
        jarvisTestSuite.getJarvisTestCases().forEach(this::runJarvisTestCase);
        jarvisTestSuite.getSuites().forEach(this::runJarvisTestSuite);
    }

    public void runJarvisTestCase(JarvisTestCase jarvisTestCase) {
        log.info("Start executing jarvisTestcase: {}", jarvisTestCase);
        initActionService.run(jarvisTestCase.getInitActionConfigs());
        executeActionService.run(jarvisTestCase.getExecutionActionConfigs());
        assertActionService.run(jarvisTestCase.getAssertActionConfigs());
        log.info("JarvisTestcase execution finished: {}", jarvisTestCase);
    }
    
    
}
