package ai.aliz.talendtestrunner.db;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.context.JarvisContextType;
import ai.aliz.jarvis.db.BigQueryExecutor;

@Component
@Primary
public class QueryExecutorComposite implements QueryExecutor {

    private final Map<JarvisContextType, QueryExecutor> queryExecutors;

    public QueryExecutorComposite(MxSQLQueryExecutor mxSQLQueryExecutor,
                                  BigQueryExecutor bigQueryExecutor) {
        this.queryExecutors = new EnumMap<>(JarvisContextType.class);
        this.queryExecutors.put(JarvisContextType.MSSQL, mxSQLQueryExecutor);
        this.queryExecutors.put(JarvisContextType.MySQL, mxSQLQueryExecutor);
//        this.queryExecutors.put(TestContextType.BigQuery, bigQueryExecutor);
    }

    @Override
    public void executeStatement(String query, JarvisContext context) {
        QueryExecutor executor = getQueryExecutor(context);
        executor.executeStatement(query, context);
    }

    @Override
    public String executeQuery(String query, JarvisContext context) {
        QueryExecutor executor = getQueryExecutor(context);
        return executor.executeQuery(query, context);
    }

    private QueryExecutor getQueryExecutor(JarvisContext context) {
        JarvisContextType contextType = context.getContextType();
        QueryExecutor executor = this.queryExecutors.get(contextType);
        if (executor == null) {
            throw new RuntimeException("No query executor registered for type: " + contextType);
        }
        return executor;
    }
}
