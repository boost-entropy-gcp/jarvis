package ai.aliz.jarvis.context;

import lombok.Getter;

import java.util.Set;

import com.google.common.collect.Sets;

public enum ContextType {
    BigQuery(Sets.newHashSet("project")),
    MySQL(Sets.newHashSet("host", "port", "database", "user", "password")),
    MSSQL(Sets.newHashSet("host", "port", "database", "user", "password")),
    TalendAPI(Sets.newHashSet("apiUrl", "apiKey")),
    SFTP(Sets.newHashSet("host", "port", "user", "password", "remoteBasePath")),
    LocalContext(Sets.newHashSet("repositoryRoot"));
    
    @Getter
    private final Set<String> requiredParameters;
    
    ContextType(Set<String> requiredParameters) {
        this.requiredParameters = requiredParameters;
    }
}
