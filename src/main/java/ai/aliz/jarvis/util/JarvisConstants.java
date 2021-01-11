package ai.aliz.jarvis.util;

public interface JarvisConstants {
    //shared
    String SOURCE_PATH = "sourcePath";
    String SOURCE_FORMAT = "sourceFormat";
    String TEST_INIT = "test_init";
    String NO_METADAT_ADDITION = "noMetadatAddition";
    String SFTP = "sftp";
    
    //Extensions
    String JSON_FORMAT = "json";
    String BQL_FORMAT = "bql";
    String SQL_FORMAT = "sql";
    
    //Application params
    //absolute path to the json file describing the contexts
    String CONTEXT = "context";
    
    //Context
    String ID = "id";
    String CONTEXT_TYPE = "contextType";
    String PARAMETERS = "parameters";
    
    //Folder
    String PRE = "pre";
    
    //MxSQL/SFTP params
    String HOST = "host";
    String USER = "user";
    String PASSWORD = "password";
    String PORT = "port";
    String DATABASE = "database";
    String REMOTE_BASE_PATH = "remoteBasePath";
    
    //BQ params
    String PROJECT = "project";
    String TABLE = "table";
    String DATASET = "dataset";
    
    //Local params
    String REPOSITORY_ROOT = "repositoryRoot";
    
    //Talend params
    String API_URL = "apiUrl";
    String API_KEY = "apiKey";
    String ENVIRONMENT = "environment";
    String WORKSPACE = "workspace";
}
