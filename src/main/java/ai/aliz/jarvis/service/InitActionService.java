package ai.aliz.jarvis.service;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.service.initiator.BQLoadInitiator;
import ai.aliz.jarvis.service.initiator.Initiator;
import ai.aliz.jarvis.service.initiator.SFTPLoadInitiator;
import ai.aliz.jarvis.service.initiator.SQLExecInitiator;
import ai.aliz.jarvis.testconfig.InitActionConfig;
import ai.aliz.jarvis.testconfig.InitActionType;

@Service
@Slf4j
public class InitActionService {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private ExecutorServiceWrapper executorService;
    
    @Autowired
    private SFTPService sftpService;
    
    private static Map<InitActionType, Class<? extends Initiator>> initActionTypeClassMap = new HashMap<>();
    
    static {
        initActionTypeClassMap.put(InitActionType.BQLoad, BQLoadInitiator.class);
        initActionTypeClassMap.put(InitActionType.SFTPLoad, SFTPLoadInitiator.class);
        initActionTypeClassMap.put(InitActionType.SQLExec, SQLExecInitiator.class);
    }
    
    public void run(List<InitActionConfig> initActionConfigs) {
        List<Runnable> initActionRunnables = initActionConfigs.stream()
                                                              .map(initActionConfig -> new Runnable() {
                                                                  @Override
                                                                  public void run() {
                                                                      InitActionService.this.run(initActionConfig);
                                                                  }
                                                              })
                                                              .collect(Collectors.toList());
        
        executorService.executeRunnablesInParallel(initActionRunnables, 5, TimeUnit.MINUTES);
    }
    
    public void run(InitActionConfig initActionConfig) {
        
        try {
            log.info("========================================================");
            log.info("Executing initaction: {}", initActionConfig);
            
            Class<? extends Initiator> initActionClass = Objects.requireNonNull(initActionTypeClassMap.get(initActionConfig.getType()));
            Initiator initAction = applicationContext.getBean(initActionClass);
            initAction.doInitAction(initActionConfig);
            
            log.info("InitAction {} finished", initActionConfig);
            log.info("========================================================");
        } catch (Exception e) {
            throw new RuntimeException(String.format("InitAction: %s failed", initActionConfig), e);
        }
    }
}
