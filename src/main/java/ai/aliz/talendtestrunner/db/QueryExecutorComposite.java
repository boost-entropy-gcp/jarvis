package ai.aliz.talendtestrunner.db;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextType;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

import static ai.aliz.talendtestrunner.context.ContextType.BigQuery;
import static ai.aliz.talendtestrunner.context.ContextType.MSSQL;
import static ai.aliz.talendtestrunner.context.ContextType.MySQL;

@Component
@Primary
public class QueryExecutorComposite implements QueryExecutor {

    private final Map<ContextType, QueryExecutor> queryExecutors;

    public QueryExecutorComposite(MxSQLQueryExecutor mxSQLQueryExecutor,
                                  BigQueryExecutor bigQueryExecutor) {
        this.queryExecutors = new EnumMap<>(ContextType.class);
        this.queryExecutors.put(MSSQL, mxSQLQueryExecutor);
        this.queryExecutors.put(MySQL, mxSQLQueryExecutor);
        this.queryExecutors.put(BigQuery, bigQueryExecutor);
    }

    @Override
    public void executeStatement(String query, Context context) {
        QueryExecutor executor = getQueryExecutor(context);
        executor.executeStatement(query, context);
    }

    @Override
    public String executeQuery(String query, Context context) {
        QueryExecutor executor = getQueryExecutor(context);
        return executor.executeQuery(query, context);
    }

    private QueryExecutor getQueryExecutor(Context context) {
        ContextType contextType = context.getContextType();
        QueryExecutor executor = this.queryExecutors.get(contextType);
        if (executor == null) {
            throw new RuntimeException("No query executor registered for type: " + contextType);
        }
        return executor;
    }
}
