package ai.aliz.jarvis.context;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

public class ContextLoader {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Getter
    private Map<String, Context> contextIdToContexts;
    
    ContextLoader(String contextPath) {
        contextIdToContexts = parseContexts(contextPath).stream().collect(Collectors.toMap(Context::getId, Function.identity()));
    }
    
    public Context getContext(String contextId) {
        return Optional.of(contextIdToContexts.get(contextId)).orElseThrow(() -> new IllegalStateException("Could not find context with id " + contextId));
    }
    
    @SneakyThrows
    private Set<Context> parseContexts(String contextPath) {
        TypeReference<Set<Context>> typeReference = new TypeReference<Set<Context>>() {};
        
        Set<Context> contexts = objectMapper.readValue(Files.asCharSource(new File(contextPath), StandardCharsets.UTF_8).read(), typeReference);
        validateContexts(contexts);
        return contexts;
    }
    
    private void validateContexts(Set<Context> contexts) {
        String errors = contexts.stream()
                                .filter(context -> !context.getParameters().keySet().containsAll(context.getContextType().getRequiredParameters()))
                                .map(context -> context.toString() + " is missing parameters. " +
                                        "Required parameters for this context type: " + String.join(",", context.getContextType().getRequiredParameters()) + "."
                                )
                                .collect(Collectors.joining("\n"));
        if (!errors.isEmpty()) {
            throw new IllegalStateException(errors);
        }
    }
}
