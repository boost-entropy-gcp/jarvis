package ai.aliz.jarvis.service.initiator;

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
        doSqlInitAction(config, contextLoader.getContext(config.getSystem()));
    }
    
    public void doSqlInitAction(InitActionConfig initActionConfig, Context context) {
        switch (context.getContextType()) {
            case MSSQL:
            case MySQL:
                mxSQLQueryExecutor.executeScript(TestRunnerUtil.getSourceContentFromConfigProperties(initActionConfig), context);
                break;
            case BigQuery:
                bigQueryExecutor.executeScript(TestRunnerUtil.getSourceContentFromConfigProperties(initActionConfig), context);
                break;
            default:
                throw new UnsupportedOperationException("Not supported context type: " + context.getContextType());
        }
    }
}

