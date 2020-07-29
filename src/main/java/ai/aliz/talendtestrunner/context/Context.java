package ai.aliz.talendtestrunner.context;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Context {
    private String id;
    private Type type;
    private Map<String, String> parameters;

    public Map<String, String> getParameters() {
        return new HashMap<>(parameters);
    }

    public String getParameter(String paramName) {
        return parameters.get(paramName);
    }
}
