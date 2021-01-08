package ai.aliz.jarvis.context;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Delegate;

import java.util.HashMap;
import java.util.Map;

@Builder
@Data
public class Context {
    
    private String id;
    private ContextType contextType;
    @Singular
    private Map<String, String> parameters;
    
    public Map<String, String> getParameters() {
        return new HashMap<>(parameters);
    }
    
    public String getParameter(String paramName) {
        return parameters.get(paramName);
    }
}
