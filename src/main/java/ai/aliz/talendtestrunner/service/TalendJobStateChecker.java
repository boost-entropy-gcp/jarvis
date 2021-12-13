package ai.aliz.talendtestrunner.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ai.aliz.jarvis.context.TestContext;
import ai.aliz.talendtestrunner.db.MxSQLQueryExecutor;

import org.junit.Assert;

@Service
@Slf4j
public class TalendJobStateChecker {
    
    ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    MxSQLQueryExecutor mxSQLQueryExecutor;
    
    private static final Set<String> ignoredProps = ImmutableSet.of("lastExecutedAt");
    
    @SneakyThrows
    public void checkJobState(String expectedStateRecord, TestContext dbContext) {
        ArrayNode expectedStateRecordArrayNode = (ArrayNode) objectMapper.readTree(expectedStateRecord);
        JsonNode expectedStateRecordNode = expectedStateRecordArrayNode.get(0);
        String job_name = expectedStateRecordNode.get("job_name").asText();
        Map<String, String> expectedStateVariables = extractStateVariables(expectedStateRecordNode);
        
        String result = getJobState(job_name, dbContext);
        
        ArrayNode resultNode = (ArrayNode) objectMapper.readTree(result);
        JsonNode actualRecordNode = resultNode.get(0);
        
        Map<String, String> actualVariables = extractStateVariables(actualRecordNode);
        
        compareVariables(expectedStateVariables, actualVariables);
    }
    
    private void compareVariables(Map<String, String> expectedStateVariables, Map<String, String> actualVariables) {
        expectedStateVariables = withoutIgnoredVariables(expectedStateVariables);
        actualVariables = withoutIgnoredVariables(actualVariables);
        
        for (Map.Entry<String, String> expectedEntry : expectedStateVariables.entrySet()) {
            String actualValue = actualVariables.get(expectedEntry.getKey());
            String expectedEntryValue = expectedEntry.getValue();
            
            Assert.assertEquals(String.format("%s property differs in expected and actual job state", expectedEntry.getKey()),
                                expectedEntryValue, actualValue);
        }
        
        Sets.SetView<String> extraInExpected = Sets.difference(expectedStateVariables.keySet(), actualVariables.keySet());
        Set<String> extraInActual = Sets.newHashSet(Sets.difference(actualVariables.keySet(), expectedStateVariables.keySet()));
        
        // These are always added, even if they're not used in the process at hand, but if we don't expect any value of them we can simply ignore them
        extraInActual.remove("maxUpdatedAt");
        extraInActual.remove("loadUntil");
        
        Joiner joiner = Joiner.on(", ");
        Assert.assertTrue("Missing variables " + joiner.join(extraInExpected), extraInExpected.isEmpty());
        Assert.assertTrue("Unexpected variables " + joiner.join(extraInActual), extraInActual.isEmpty());
        
    }
    
    private Map<String, String> withoutIgnoredVariables(Map<String, String> originalVariables) {
        HashMap<String, String> withoutIgnored = Maps.newHashMap(originalVariables);
        Iterator<Map.Entry<String, String>> it = withoutIgnored.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (ignoredProps.contains(entry.getKey())) {
                log.info("Removing ignored state variable {}", entry.getKey());
                it.remove();
            }
        }
        
        return withoutIgnored;
    }
    
    private Map<String, String> extractStateVariables(JsonNode stateRecordNode) throws IOException {
        String stateJsonField = stateRecordNode.get("state_json").asText();
        JsonNode expectedStateFieldNode = objectMapper.readTree(stateJsonField);
        
        Map<String, String> result = Maps.newHashMap();
        
        ArrayNode stateVariables = (ArrayNode) expectedStateFieldNode.get("state");
        for (JsonNode stateVariable : stateVariables) {
            String key = stateVariable.get("key").asText();
            String value = stateVariable.get("value").asText();
            
            result.put(key, value);
        }
        
        return result;
    }
    
    public String getJobState(String jobName, TestContext dbContext) {
        return mxSQLQueryExecutor.executeQuery(String.format("SELECT * FROM talend.talend_job_state WHERE job_name='%s'", jobName), dbContext);
    }
}
