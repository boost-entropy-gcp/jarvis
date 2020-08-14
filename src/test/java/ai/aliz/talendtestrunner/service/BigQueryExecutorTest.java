package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.context.Context;
import ai.aliz.talendtestrunner.context.ContextLoader;
import ai.aliz.talendtestrunner.db.BigQueryExecutor;
import com.google.api.gax.paging.Page;
import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Ignore
public class BigQueryExecutorTest {
    private static final String API_URL = "apiUrl";
    private static final String CONTEXT_PATH = "C:\\Users\\bberr\\git\\aliz\\jarvis\\src\\test\\resources\\contexts.json";

    @Autowired
    private BigQueryExecutor bigQueryExecutor;

    @Autowired
    private ContextLoader contextLoader;

    @MockBean
    private BigQuery bigQuery;

    @Test
    @SneakyThrows
    public void testSampleQuery() {
        TableResult tableResult = getTableResult();
        Mockito.when(bigQuery.query(Mockito.any())).thenReturn(tableResult);
        contextLoader.parseContext(CONTEXT_PATH);
        Context bqContext = contextLoader.getContext("EDW");
        String result = bigQueryExecutor.executeQuery("SELECT * FROM `{{project}}.tf_test.tf_test3`", bqContext);
    }

    public TableResult getTableResult() {
        Schema schema = Schema.of(
                Field.of("test_id", LegacySQLTypeName.INTEGER),
                Field.of("test", LegacySQLTypeName.STRING));
        FieldValue fieldValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, 1);
        FieldValue fieldValue2 = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "test");
        FieldValueList fieldValues = FieldValueList.of(List.of(fieldValue, fieldValue2));
        Page<FieldValueList> page = new PageImpl<>(null, "c", Arrays.asList(fieldValues));
        return new TableResult(schema, 1L, page);
    }

}
