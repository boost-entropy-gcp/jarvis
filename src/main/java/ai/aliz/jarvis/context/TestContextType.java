package ai.aliz.jarvis.context;

import lombok.Getter;

import java.util.Set;

import com.google.common.collect.Sets;

import static ai.aliz.jarvis.util.JarvisConstants.API_KEY;
import static ai.aliz.jarvis.util.JarvisConstants.API_URL;
import static ai.aliz.jarvis.util.JarvisConstants.DATABASE;
import static ai.aliz.jarvis.util.JarvisConstants.ENVIRONMENT;
import static ai.aliz.jarvis.util.JarvisConstants.HOST;
import static ai.aliz.jarvis.util.JarvisConstants.PASSWORD;
import static ai.aliz.jarvis.util.JarvisConstants.PORT;
import static ai.aliz.jarvis.util.JarvisConstants.PROJECT;
import static ai.aliz.jarvis.util.JarvisConstants.REMOTE_BASE_PATH;
import static ai.aliz.jarvis.util.JarvisConstants.REPOSITORY_ROOT;
import static ai.aliz.jarvis.util.JarvisConstants.USER;
import static ai.aliz.jarvis.util.JarvisConstants.WORKSPACE;

public enum TestContextType {
    BigQuery(Sets.newHashSet(PROJECT)),
    MySQL(Sets.newHashSet(HOST, PORT, DATABASE, USER, PASSWORD)),
    MSSQL(Sets.newHashSet(HOST, PORT, DATABASE, USER, PASSWORD)),
    PostgreSQL(Sets.newHashSet(HOST, PORT, DATABASE, USER, PASSWORD)),
    TalendAPI(Sets.newHashSet(API_URL, API_KEY, ENVIRONMENT, WORKSPACE)),
    SFTP(Sets.newHashSet(HOST, PORT, USER, PASSWORD, REMOTE_BASE_PATH)),
    LocalContext(Sets.newHashSet(REPOSITORY_ROOT));
    
    @Getter
    private final Set<String> requiredParameters;
    
    TestContextType(Set<String> requiredParameters) {
        this.requiredParameters = requiredParameters;
    }
}
