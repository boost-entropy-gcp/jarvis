package ai.aliz.talendtestrunner.db;

import ai.aliz.talendtestrunner.context.Context;

public interface QueryExecutor {

    void executeStatement(String query, Context context);

    String executeQuery(String query, Context context);
}
