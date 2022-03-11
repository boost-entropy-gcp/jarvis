package ai.aliz.jarvis.service.initaction.initiator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.context.JarvisContextLoader;
import ai.aliz.jarvis.db.BigQueryExecutor;
import ai.aliz.jarvis.db.JDBCSQLQueryExecutor;
import ai.aliz.jarvis.config.InitActionConfig;
import ai.aliz.jarvis.util.JarvisUtil;

@Service
public class SQLExecInitiator implements Initiator {
    
    @Autowired
    private JarvisContextLoader contextLoader;
    
    @Autowired
    private JDBCSQLQueryExecutor JDBCSQLQueryExecutor;
    
    @Autowired
    private BigQueryExecutor bigQueryExecutor;
    
    @Override
    public void doInitAction(InitActionConfig config) {
        doInitActionInner(config, contextLoader.getContext(config.getSystem()));
    }
    
    private void doInitActionInner(InitActionConfig initActionConfig, JarvisContext context) {
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

