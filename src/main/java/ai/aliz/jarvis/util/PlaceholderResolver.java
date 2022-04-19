package ai.aliz.jarvis.util;

import lombok.experimental.UtilityClass;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

@UtilityClass
public class PlaceholderResolver {

    private static final Pattern MISSING_PLACEHOLDER_REGEX = Pattern.compile("^.*\\{\\{.*?}}.*$", Pattern.MULTILINE);
    
    public String resolve(String pattern, Map<String, String> parameters) {
        String result = pattern;
        Iterator<Map.Entry<String, String>> parameterIterator = parameters.entrySet().iterator();
        while (parameterIterator.hasNext()) {
            Map.Entry<String, String> kv = parameterIterator.next();
            result = result.replace("{{" + kv.getKey() + "}}", kv.getValue());
        }
        if (MISSING_PLACEHOLDER_REGEX.matcher(result).find()) {
            throw new IllegalStateException("Some placeholders have not been resolved in: '" + result + "'");
        }
        return result;
    }
}
