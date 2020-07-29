package ai.aliz.talendtestrunner.service;

import ai.aliz.talendtestrunner.config.AppConfig;
import ai.aliz.talendtestrunner.context.Context;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class TalendApiServiceTest {
    private static final String API_URL = "apiUrl";

    @Autowired
    private TalendApiService talendApiService;

    @Autowired
    private AppConfig appConfig;

    @Test
    public void runAnyApp() {
        Context context = new Context();

        Map<String, String> parameters = Maps.newHashMap();
        parameters.put(API_URL, "https://api.ap.cloud.talend.com/tmc/v1.3");
        parameters.put("workspace", "");
        parameters.put("environment", "");
        parameters.put("apiKey", "");
        context.setParameters(parameters);
        talendApiService.executeTask("SERVICE_CASE", context);
    }

}
