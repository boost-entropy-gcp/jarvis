package ai.aliz.jarvis.context;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@JsonDeserialize(builder = Context.ContextBuilder.class)
public class Context {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("contextType")
    private ContextType contextType;
    
    @Singular
    @JsonProperty("parameters")
    private Map<String, String> parameters;
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class ContextBuilder {}
    
    public Map<String, String> getParameters() {
        return new HashMap<>(parameters);
    }
    
    public String getParameter(String paramName) {
        return parameters.get(paramName);
    }
}
