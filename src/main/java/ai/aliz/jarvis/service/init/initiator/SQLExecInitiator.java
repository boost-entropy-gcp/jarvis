package ai.aliz.jarvis.service.init.initiator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.Context;
import ai.aliz.jarvis.context.ContextLoader;
import ai.aliz.jarvis.db.BigQueryExecutor;
import ai.aliz.jarvis.db.MxSQLQueryExecutor;
import ai.aliz.jarvis.testconfig.InitActionConfig;
import ai.aliz.jarvis.util.TestRunnerUtil;

@Service
public class SQLExecInitiator implements Initiator {
    
    @Autowired
    private ContextLoader contextLoader;
    
    @Autowired
    private MxSQLQueryExecutor mxSQLQueryExecutor;
    
    @Autowired
    private BigQueryExecutor bigQueryExecutor;
    
    @Override
    public void doInitAction(InitActionConfig config) {
        doInitActionInner(config, contextLoader.getContext(config.getSystem()));
    }
    
    private void doInitActionInner(InitActionConfig initActionConfig, Context context) {
        String sourceContent = TestRunnerUtil.getSourceContentFromConfigProperties(initActionConfig);
        switch (context.getContextType()) {
            case MSSQL:
            case MySQL:
                mxSQLQueryExecutor.executeScript(sourceContent, context);
                break;
            case BigQuery:
                bigQueryExecutor.executeScript(sourceContent, context);
                break;
            default:
                throw new UnsupportedOperationException("Not supported context type: " + context.getContextType());
        }
    }
}

