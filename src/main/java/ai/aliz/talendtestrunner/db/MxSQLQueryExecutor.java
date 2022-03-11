package ai.aliz.talendtestrunner.db;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.PreDestroy;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.springframework.stereotype.Component;

import ai.aliz.jarvis.context.JarvisContext;
import ai.aliz.jarvis.context.JarvisContextType;
import ai.aliz.jarvis.util.PlaceholderResolver;

@Component
@AllArgsConstructor
@Slf4j
public class MxSQLQueryExecutor implements QueryExecutor {

    private static final String MS_CONNECTION_STRING_PATTERN =
            "jdbc:sqlserver://{{host}}:{{port}};databaseName={{database}};user={{user}};password={{password}}";

    private static final String My_CONNECTION_STRING_PATTERN =
            "jdbc:mysql://{{host}}:{{port}}/{{database}}?user={{user}}&password={{password}}";

    private PlaceholderResolver placeholderResolver;

    private Map<JarvisContext, Connection> connectionMap = Maps.newHashMap();


    public void executeScript(String query, JarvisContext context) {
        String[] splits = query.split(";");
        for (String split : splits) {
            String trimmed = split.trim();
            if (!trimmed.isEmpty()) {
                executeStatement(trimmed, context);
            }
        }
    }

    @PreDestroy
    public void tearDownConnections() {
        for (Connection connection : connectionMap.values()) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("Error while closing connection", e);
            }
        }

    }

    @Override
    @SneakyThrows
    public void executeStatement(String query, JarvisContext context) {
        doWithStatement(query, context, preparedStatement -> executeStatement(preparedStatement));
    }

    private <T> T doWithStatement(String query, JarvisContext context, Function<PreparedStatement, T> statementAction) {
        Connection connection = getConnectionForContext(context);
        String completedQuery = placeholderResolver.resolve(query, context.getParameters());

        try {
            log.info("Executing query {}", completedQuery);
            PreparedStatement preparedStatement = connection.prepareStatement(completedQuery);
            return statementAction.apply(preparedStatement);
        } catch (Exception e) {
            log.error("Failed to executeStatement query: " + completedQuery, e);
            throw new RuntimeException(e);
        }
    }

    private Connection getConnectionForContext(JarvisContext context) {
        Connection connection = connectionMap.get(context);
        if (connection == null) {
            String connectionUrl = placeholderResolver.resolve(getConnectionPattern(context), context.getParameters());
            try {
                connection = DriverManager.getConnection(connectionUrl);
                connectionMap.put(context, connection);
            } catch (SQLException e) {
                throw new IllegalStateException("Error while setting up connection.", e);
            }
        }
        return connection;
    }

    @SneakyThrows
    private boolean executeStatement(PreparedStatement preparedStatement) {
        return preparedStatement.execute();
    }

    private String getConnectionPattern(JarvisContext context) {
        JarvisContextType contextType = context.getContextType();
        switch (contextType) {
            case MySQL:
                return My_CONNECTION_STRING_PATTERN;
            case MSSQL:
                return MS_CONNECTION_STRING_PATTERN;
            default:
                throw new IllegalArgumentException("Unsupported connection type: " + contextType);
        }
    }

    @Override
    public String executeQuery(String query, JarvisContext context) {
        return doWithStatement(query, context, this::queryStatementForJsonResult);
    }

    @SneakyThrows
    private String queryStatementForJsonResult(PreparedStatement statement) {
        ResultSet resultSet = statement.executeQuery();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        Gson gson = new GsonBuilder().create();
        JsonArray result = new JsonArray();

        while (resultSet.next()) {
            JsonObject objectForRow = new JsonObject();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object columnValue = resultSet.getObject(i);
                objectForRow.add(columnName, gson.toJsonTree(columnValue));
            }

            result.add(objectForRow);
        }

        return gson.toJson(result);
    }
}
