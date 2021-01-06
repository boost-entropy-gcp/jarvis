package ai.aliz.jarvis.context;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

import org.springframework.stereotype.Component;

@Component
public class ContextLoader {
    
    private final ObjectMapper objectMapper;
    
    public ContextLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Getter
    private Set<Context> contexts;
    
    @SneakyThrows
    public void parseContext(String contextPath) {
        TypeReference<Set<Context>> typeReference = new TypeReference<Set<Context>>() {};
        
        contexts = objectMapper.readValue(Files.asCharSource(new File(contextPath), StandardCharsets.UTF_8).read(), typeReference);
    }
    
    public Context getContext(String contextId) {
        return contexts.stream().filter(c -> Objects.equals(c.getId(), contextId)).findAny().orElseThrow(
                () -> new IllegalStateException("Could not find context with id " + contextId)
        );
    }
}
