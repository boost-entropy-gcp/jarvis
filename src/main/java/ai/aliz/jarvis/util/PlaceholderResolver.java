package ai.aliz.jarvis.util;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;

@Component
public class PlaceholderResolver {

    public String resolve(String pattern, Map<String, String> parameters) {
        String result = pattern;
        Iterator<Map.Entry<String, String>> parameterIterator = parameters.entrySet().iterator();
        while (parameterIterator.hasNext()) {
            Map.Entry<String, String> kv = parameterIterator.next();
            result = result.replace("{{" + kv.getKey() + "}}", kv.getValue());
        }
        return result;
    }
}
