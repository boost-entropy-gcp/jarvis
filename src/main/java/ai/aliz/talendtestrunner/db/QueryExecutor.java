package ai.aliz.talendtestrunner.db;

import ai.aliz.jarvis.context.TestContext;

public interface QueryExecutor {

    void executeStatement(String query, TestContext context);

    String executeQuery(String query, TestContext context);
}
