package ai.aliz.jarvis.service.assertaction;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import ai.aliz.jarvis.context.JarvisContextLoader;
import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.context.JarvisContextType;

import ai.aliz.jarvis.config.AssertActionConfig;

@Component
@Slf4j
public class AssertActionService {
    
    @Autowired
    private JarvisContextLoader contextLoader;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private static Map<JarvisContextType, Class<? extends Assertor>> assertActionTypeMap = new HashMap<>();
    
    static {
        assertActionTypeMap.put(JarvisContextType.BigQuery, BqAssertor.class);
        assertActionTypeMap.put(JarvisContextType.MySQL, MySQLAssertor.class);
    }
    
    public void run(List<AssertActionConfig> assertActionConfigList) {
        assertActionConfigList.forEach(this::run);
    }
    
    public void run(AssertActionConfig assertActionConfig) {
        log.info("Starting assert action: {}", assertActionConfig);
        JarvisContext context = contextLoader.getContext(assertActionConfig.getSystem());
        Class<? extends Assertor> assertActionClass = null;
        
        assertActionClass = Objects.requireNonNull(assertActionTypeMap.get(context.getContextType()));
        
        Assertor assertor = applicationContext.getBean(assertActionClass);
        assertor.doAssert(assertActionConfig);
        log.info("Assert action finished: {}", assertActionConfig);
    }
}
