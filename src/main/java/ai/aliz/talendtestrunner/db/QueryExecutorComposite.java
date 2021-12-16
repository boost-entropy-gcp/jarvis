package ai.aliz.talendtestrunner.db;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.jarvis.context.TestContextType;
import ai.aliz.jarvis.db.BigQueryExecutor;

@Component
@Primary
public class QueryExecutorComposite implements QueryExecutor {

    private final Map<TestContextType, QueryExecutor> queryExecutors;

    public QueryExecutorComposite(MxSQLQueryExecutor mxSQLQueryExecutor,
                                  BigQueryExecutor bigQueryExecutor) {
        this.queryExecutors = new EnumMap<>(TestContextType.class);
        this.queryExecutors.put(TestContextType.MSSQL, mxSQLQueryExecutor);
        this.queryExecutors.put(TestContextType.MySQL, mxSQLQueryExecutor);
//        this.queryExecutors.put(TestContextType.BigQuery, bigQueryExecutor);
    }

    @Override
    public void executeStatement(String query, TestContext context) {
        QueryExecutor executor = getQueryExecutor(context);
        executor.executeStatement(query, context);
    }

    @Override
    public String executeQuery(String query, TestContext context) {
        QueryExecutor executor = getQueryExecutor(context);
        return executor.executeQuery(query, context);
    }

    private QueryExecutor getQueryExecutor(TestContext context) {
        TestContextType contextType = context.getContextType();
        QueryExecutor executor = this.queryExecutors.get(contextType);
        if (executor == null) {
            throw new RuntimeException("No query executor registered for type: " + contextType);
        }
        return executor;
    }
}
