package ai.aliz.talendtestrunner.testconfig;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextType;
import ai.aliz.talendtestrunner.service.AssertServiceTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.aliz.talendtestrunner.context.ContextLoader;

import static org.hamcrest.Matchers.is;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Objects;

@Ignore
public class TestConfigParseTest {
    
    @Test
    public void testConfig() throws Exception {
        
        final ContextLoader contextLoader = new ContextLoader(new ObjectMapper());
        String path = new File(Objects.requireNonNull(AssertServiceTest.class.getClassLoader().getResource("sample_context.json").getFile())).getPath();
        contextLoader.parseContext(path);

        Context context = contextLoader.getContext("SFTP-password");
        Assert.assertThat(context.getParameter("password"), is("{password}"));
        Assert.assertThat(context.getContextType(), is(ContextType.SFTP));
        Assert.assertThat(context.getParameters().size(), is(5));
    }
}
