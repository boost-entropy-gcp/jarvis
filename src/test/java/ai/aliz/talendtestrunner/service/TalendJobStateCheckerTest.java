package ai.aliz.talendtestrunner.service;

import ai.aliz.jarvis.context.JarvisContextLoader;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Ignore
public class TalendJobStateCheckerTest {
    private static final String API_URL = "apiUrl";

    @Autowired
    private TalendJobStateChecker talendJobStateChecker;

    @Autowired
    private JarvisContextLoader contextLoader;

    @Test
    public void sampleTest() throws Exception {
        talendJobStateChecker.checkJobState("[{\"project_name\":\"\", \"job_name\":\"Case2Ods\", \"state_json\":\"{\\\"state\\\":[{\\\"key\\\":\\\"lastExecutedAt\\\",\\\"value\\\":\\\"2019-11-08 11:17:33.134\\\"},{\\\"key\\\":\\\"maxUpdatedAt\\\",\\\"value\\\":\\\"2019-11-07 10:59:52.000\\\"},{\\\"key\\\":\\\"loadUntil\\\",\\\"value\\\":\\\"2100-01-01 00:00:00.000\\\"}]}\"}]",
                contextLoader.getContext("TalendDb"));
    }

}
