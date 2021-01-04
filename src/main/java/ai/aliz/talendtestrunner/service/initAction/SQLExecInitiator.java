package ai.aliz.talendtestrunner.service.initAction;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import ai.aliz.talendtestrunner.db.MxSQLQueryExecutor;
import ai.aliz.talendtestrunner.testconfig.InitActionConfig;
import ai.aliz.talendtestrunner.util.TestRunnerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
