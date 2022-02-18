package ai.aliz.jarvis.integration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Preconditions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ai.aliz.jarvis.context.TestContextLoader;
import ai.aliz.jarvis.service.initaction.InitActionService;
import ai.aliz.jarvis.testconfig.InitActionConfig;
import ai.aliz.jarvis.testconfig.InitActionConfigFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = "context=src/test/resources/integration/bq-context.json")
public class TestInitActionServiceBQIntegration {
    
    /*
    PREREQUISITES
     * The test requires an existing GCP project with BQ API enabled.
     * To provide resources for this test, apply the Terraform configurations in the integration folder.
    */
    
    private static final String DATASET_NAME = "jarvis_test";
    private static final DatasetId DATASET_ID = DatasetId.of(DATASET_NAME);
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    
    @Autowired
    private InitActionConfigFactory initActionConfigFactory;
    
    @Autowired
    private InitActionService actionService;
    @Autowired
    private TestContextLoader contextLoader;
 
    
    private BigQuery bigQuery;
    
    @Before
    public void init() {
//        bigQuery = bigQueryService.createBigQueryClient(contextLoader.getContext("BQ"));
        Preconditions.checkNotNull(bigQuery.getDataset(DATASET_ID));
    }
    
    @Test
    public void testWithScript() {
        final String tableName = "init_create_table_test";
        final TableId tableId = TableId.of(DATASET_NAME, tableName);
        Preconditions.checkArgument(Objects.isNull(bigQuery.getTable(tableId)));
        
        try {
            List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/integration/bq-script"));
            actionService.run(actionConfigs);
            
            assertTrue(bigQuery.getTable(tableId).exists());
        } finally {
            bigQuery.delete(tableId);
        }
    }
    
    @Test
    public void testWithJSON() {
        final String table_name = "init_insert_json_data_test";
        final TableId tableId = TableId.of(DATASET_NAME, table_name);
        bigQuery.create(TableInfo.newBuilder(tableId, StandardTableDefinition.of(Schema.of(Field.newBuilder("text", StandardSQLTypeName.STRING).build()))).build());
        Preconditions.checkNotNull(bigQuery.getTable(tableId));
        
        try {
            List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/integration/bq-json"));
            actionService.run(actionConfigs);
            
            TableResult result = bigQuery.getTable(tableId).list();
            String expectedTextValue = "dummy text";
            assertEquals(1, result.getTotalRows());
            assertTrue(StreamSupport.stream(result.iterateAll().spliterator(), false)
                                    .anyMatch(valueList -> valueList.get(0).getStringValue().equals(expectedTextValue)));
        } finally {
            bigQuery.delete(tableId);
        }
    }
    
    @Test
    public void testInvalidScript() {
        exceptionRule.expect(ExecutionException.class);
        
        List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/integration/bq-script-invalid"));
        actionService.run(actionConfigs);
    }
    
    @Test
    public void testMissingTableJSONInsert() {
        exceptionRule.expect(ExecutionException.class);
        
        List<InitActionConfig> actionConfigs = initActionConfigFactory.getInitActionConfigs(new HashMap<>(), new File("src/test/resources/integration/bq-json-missing-table"));
        actionService.run(actionConfigs);
    }
}
