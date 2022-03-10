package ai.aliz.talendtestrunner.db;

import ai.aliz.jarvis.context.JarvisContext;

public interface QueryExecutor {

    void executeStatement(String query, JarvisContext context);

    String executeQuery(String query, JarvisContext context);
}
