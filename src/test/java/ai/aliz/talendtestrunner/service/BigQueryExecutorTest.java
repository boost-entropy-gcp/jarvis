package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Ignore
public class BigQueryExecutorTest {
    private static final String API_URL = "apiUrl";

    @Autowired
    private BigQueryExecutor bigQueryExecutor;

    @Autowired
    private ContextLoader contextLoader;

    @Test
    public void testSampleQuery() {
        Context bqContext = contextLoader.getContext("BigQuery");

        String result = bigQueryExecutor.executeQuery("SELECT * FROM EDW_CORE.SERVICE_CASE", bqContext);
        System.out.println(result);
    }

}
