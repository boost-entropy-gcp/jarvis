package ai.aliz.talendtestrunner.context;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@Ignore
public class ContextLoaderTest {

    @Autowired
    private ContextLoader underTest;

    @Test
    public void shouldParseContextsJson() {
//        underTest.parseContext();
//        Set<Context> contexts = underTest.getContexts();
//        assertThat(contexts, IsCollectionWithSize.hasSize(5));
    }
}
