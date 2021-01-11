package ai.aliz.jarvis.db;

import ai.aliz.jarvis.context.Context;

public interface QueryExecutor {
    
    void executeStatement(String query, Context context);
    
    String executeQuery(String query, Context context);
    
    void executeScript(String query, Context context);
}
