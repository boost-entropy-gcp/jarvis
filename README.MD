# Jarvis Test Runner

## Package
To package the test runner application into a JAR run 
```
mvn clean package spring-boot:repackage
```
in the command line. 
This will create three JAR files.
The _jarvis-0.0.1-SNAPSHOT.jar_ is a distributable JAR made by Maven.
The _jarvis-0.0.1-SNAPSHOT-spring-boot.jar_ is the previously mentioned JAR repackaged by Spring Boot to make it executable from the command line.
The Spring Boot Maven plugin also creates a copy of the Maven JAR named _jarvis-0.0.1-SNAPSHOT.jar.original_. 
Run the application with this command.
```
java -jar target/jarvis-0.0.1-SNAPSHOT-spring-boot.jar --context="arg1""arg1" "arg2"
```
---
**NOTE**

The name of the JAR will be the name in the pom.xml concatenated to the version from the pom.xml by a '-'.

---
## Context
A context represents how the test runner can reach local and external resources that are necessary for the tests. 
Contexts are parsed from the __contexts.json__ file by the ContextLoader.
The path to the __context.json__ is a required parameter (```--context```) to run the application.
Each context has an id which correlates to the context's type shown in the examples below.
Valid context types are BigQuery, MySQL, MSSQL, TalendAPI, SFTP and LocalContext.
Beside id and type, the context can have various parameters depending on its type in key-value format.  
Let's see a valid example for a context of each type.

### BigQuery

```
[
  {
    "id": "{same as the folder name in the assert and pre folders}",
    "contextType": "BigQuery",
    "parameters": {
      "project": "scoot-edw-dev",
      "datasetNamePrefix": "csk_",
      "edwCoreDataset": "EDW_CORE",
      "stagingDataset": "STAGING"
    }
  }
]
```
For the BigQuery type the _project_ is a required parameter, while _datasetNamePrefix_ is optional.  
Any other parameters can be used as substitution variables in SQL files (when the key is used it is replaced by the value during query execution).  

### LocalContext

```
[
  {
    "id": "local",
    "contextType": "LocalContext",
    "parameters": {
      "repositoryRoot": "{absolute path of the project folder}"
    }
  }
]
```
The local context type requires a _repositoryRoot_ parameter, which is used for BQ script testing.
The parameter indicates where the SQL files can be found locally, so later, in the test definition the relative path to the repository root is usable.

### MySQL

```
[
  {
    "id": "{name of the sql file}",
    "contextType": "MySQL",
    "parameters": {
      "host": "{db host}",
      "port": "{db port}",
      "database": "{database}",
      "user": "{username}",
      "password": "{password}"
    }
  }
]
```
The MySQL context type has parameters that describe how to connect to the MySQL DB.

### MSSQL

```
[
  {
    "id": "{id, name of folder}",
    "contextType": "MSSQL",
    "parameters": {
      "host": "{db host}",
      "port": "{db port}",
      "database": "{database}",
      "user": "{username}",
      "password": "{password}"
    }
  }
]
```
The MSSQL context type has parameters that describe how to connect to the MySQL DB.

### SFTP

```
[
  {
    "id": "SFTP-password",
    "contextType": "SFTP",
    "parameters": {
      "host": "{db host}",
      "port": "{db port}",
      "user": "{username}",
      "password": "{password}",
      "remoteBasePath": "/out"
    }
  }
]
```
The SFTP context type has parameters that describe how to connect to the MySQL DB.

### TalendAPI

```
[
  {
    "id": "TalendAPI",
    "contextType": "TalendAPI",
    "parameters": {
      "apiUrl": "{talend API url}",
      "apiKey": "{API key}",
      "environment": "{environment}",
      "workspace": "{workspace}"
    }
  }
]
```
The TalendAPI context type requires an _apiUrl_ and an _apiKey_ parameter to connect with the API, while _environment_ and _workspace_ are optional parameters that can be substituted into queries.

---
### Test BQ view

###### Folder system:
Folder system should look like this. The test going through the folders and find the necessary files.

1. test_view (= CONFIG_PATH)
    * test_case1
        * assert
            * _{id from contexts.json}_
                * _{bigQuery dataset}_
                    * test_view.json
        * pre
            * _{id from contexts.json}_ 
                * _{bigQuery dataset}_ 
                    * _{bigQuery table}_.json
            * _{id from context.json}_.sql      
    * test_case2
    * testSuite.json 
2. test_bqQuery
    
_{bigQuery table}_.json and _{id from context.json}_.sql  two ways to add data to bigQuery table.

.json example:
```
[
    {
        "id" : 1,
        "first_name": "fName1",
        "last_name": "lName1"
    }
]
```

.sql example to insert data to BQ table:
```
DELETE FROM `{project}.{dataset}.{table}` WHERE 1=1;

INSERT INTO `{project}.{dataset}.{table}` (user_id, age)
VALUES (1, 1);
```

json file in assert folder contains object(s) from the BQ view in json format. 

example:
```
[
  {
    "first_name": "fName1",
    "last_name": "lName1",
    "age": "1"
  }
]
```


testSuite.json example
```
{
  "caseAutoDetect": true
}
```

###### contexts json

context.json example

contexts.json (= CONTEXT_PATH)

Make contexts.json file in test/resources to define parameters for the tests.
```
[
  {
    "id": {id},
    "contextType": "BigQuery",
    "parameters": {
      "project": {gcp project name}
    }
  }
]
```

##### To run test for view:

1. Set up the folder system with the files, pay attention of naming the folders and files, the json format and data in the json files.
2. In IntegrationTestRunner.class set the CONTEXTS_PATH and the CONFIG_PATH. CONTEXTS_PATH is the absolute path of the contexts.json. CONFIG_PATH is the absolute path of the folder which has the test cases.
3. Run IntegrationTestRunner.class


### Test BQ query

###### Folder system
Same as it was at the bq view tests.

1. test_view
2. test_bqQuery (= CONFIG_PATH)
    * test_case1
        * assert
            * _{id from contexts.json}_
                * _{bigQuery dataset}_
                    * test_view.json
        * pre
            * _{id from contexts.json}_ 
                * _{bigQuery dataset}_ 
                    * _{bigQuery table}_.json
            * _{id from context.json}_.sql      
    * test_case2
    * testSuite.json 
3. one or more .sql file somewhere in the project (testSuit.json queryPath) to make Bq table modification(s).
    
Load to the BQ table and assert data are the same as it was at the bq view test.



In BqQuery test the testSuit.json contains one or more BqQuery execution. contextType is always BqQuery, queryPath is the path feom the project folder to the .sql file.

testSuite.json example
```
{
  "caseAutoDetect": true,
   "executions":[
       {
           "executionType": "BqQuery",
           "queryPath": "\\src\\test\\resources\\test.sql", (example)
           "executionContext": "{context id}"
       }
     ]
}
```

###### contexts json

context.json example

contexts.json (= CONTEXT_PATH)

Make contexts.json file in test/resources to define parameters for the tests. LocalContext is for read local files, and the BigQuery context is for make the bigQuery job. The repositoryPath is the absolute path on the project folder. The test get the .sql file absolute path from the repositoryPath + testSuit.json queryPath.
```
[
  {
    "id": {id},
    "contextType": "BigQuery",
    "parameters": {
      "project": {gcp project name}
    }
  }
  {
    "id": {id},
    "contextType": "LocalContext",
    "parameters": {
      "repositoryRoot": {project folder path}
    }
  }
]
```

##### To run test for BqQuery:

1. Set up the folder system with the files, pay attention of naming the folders and files, the json format and data in the json files.
2. In IntegrationTestRunner.class set the CONTEXTS_PATH and the CONFIG_PATH. CONTEXTS_PATH is the absolute path of the contexts.json. CONFIG_PATH is the absolute path of the folder which has the test cases.
3. Set up the contexts.json with LocalContext and add execution to the testSuit.json. LocalContext is for read local files.
4. Run IntegrationTestRunner.class

#### testSuits.json

1. caseAutoDetect
    * boolean
        * If it's true the test will make initActionConfigs and assertActionConfigs.
        
        
###### executions:

1. execution
    * List of
        * executionType (type of execution. _BigQuery, Airflow, Talend, NoOps_)
        * queryPath (path of the file for do the execution)
        * executionContext (context id)
2. 
###### defaultProperties:

1. assert.excludePreviouslyInsertedRows
    * boolean
        * Deafult value is false. If it's true it puts "WHERE {tableName}_INSERTED_BY != 'test_init'" to the query for assert.
2. assert.assertKeyColumns
    * list -> default {tableName}_BID, {tableName}_VALID_FROM
        *
3. init.{contextId}.noMetadatAddition
    * boolean
        * Default value is true. If it's false the test add a {tableName}_INSERTED_BY field with "test_init" value to the table which load by json file.