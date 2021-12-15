package ai.aliz.jarvis.service.initaction.initiator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.db.BigQueryExecutor;
import ai.aliz.jarvis.db.JDBCSQLQueryExecutor;
import ai.aliz.jarvis.testconfig.InitActionConfig;
import ai.aliz.jarvis.util.JarvisUtil;

@Service
public class SQLExecInitiator implements Initiator {
    
    @Autowired
    private TestContextLoader contextLoader;
    
    @Autowired
    private JDBCSQLQueryExecutor JDBCSQLQueryExecutor;
    
    @Autowired
    private BigQueryExecutor bigQueryExecutor;
    
    @Override
    public void doInitAction(InitActionConfig config) {
        doInitActionInner(config, contextLoader.getContext(config.getSystem()));
    }
    
    private void doInitActionInner(InitActionConfig initActionConfig, TestContext context) {
        String sourceContent = JarvisUtil.getSourceContentFromConfigProperties(initActionConfig);
        switch (context.getContextType()) {
            case MSSQL:
            case PostgreSQL:
            case MySQL:
                JDBCSQLQueryExecutor.executeBQInitializatorScript(sourceContent, context);
                break;
            case BigQuery:
                bigQueryExecutor.executeBQInitializatorScript(sourceContent, context);
                break;
            default:
                throw new UnsupportedOperationException("Not supported context type: " + context.getContextType());
        }
    }
}

