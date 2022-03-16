package ai.aliz.jarvis.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;

@RunWith(JUnit4.class)
public class TestPlaceholderResolver {

    @Test
    public void shouldResolvePlaceholders() {
        PlaceholderResolver undertest = new PlaceholderResolver();
        String query = "select * from {{table}} where {{condition}}={{condition}};";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("table", "REPLACED_TABLE");
        parameters.put("condition", "1");
        Assert.assertThat(undertest.resolve(query, parameters), is("select * from REPLACED_TABLE where 1=1;"));
    }
}