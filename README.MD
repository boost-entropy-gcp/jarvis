# Jarvis Test Runner

## Package
To package the test runner application into a JAR run 
```
mvn clean package spring-boot:repackage
```
in the command line. 
This will create 2 JAR files.
The _target/jarvis_ is the JAR repackaged by Spring Boot to make the application executable from the command line.
The _target/jarvis.jar.original_ is a distributable JAR made by Maven. 
Run the application with this command.
```
java -jar target/jarvis.jar --context="arg1""arg1" "arg2"
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
    "id": "{id}",
    "contextType": "BigQuery",
    "parameters": {
      "project": "project_name",
      "datasetNamePrefix": "dev_"
    }
  }
]
```
For the BigQuery type the _project_ is a required parameter.  
The _datasetNamePrefix_ is an optional parameter that can be substituted into BQ queries.
Using dataset name prefixes is a naming convention, which allows to easily swap between similar, near exact copies of a dataset.
For example dev_core and staging_core, where dev_ and staging_ are the dataset name prefixes.

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
    "id": "{id}",
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
    "id": "{id}",
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
The MSSQL context type has parameters that describe how to connect to the MSSQL DB.

### SFTP

```
[
  {
    "id": "{id}",
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
The SFTP context type has parameters that describe how to connect to the SFTP server.

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
The TalendAPI context type requires an _apiUrl_ and an _apiKey_ parameter to connect to the API, and it also requires an _environment_ and a _workspace_ parameter.

Any other parameters not listed here can be used as substitution variables in files (when the key is used it is replaced by the value during query execution).

---
## Steps
When the application runs, it completes three phases. 
The first is the __initiation__ step, the next is the test __execution__ and finally comes the __assertion__.
This order never changes and each phase starts only after the previous has finished.
This part of the documentation details the role of each step and how the events can be customized during the steps.

### Initiation Step
The initiation step prepares the environment for the upcoming test execution. 
During this phase the test runner can automatically create the prerequisites of the test.
For example, it can load data or clean up after a previous run.
If the initiation is correctly set up, then every condition will be provided to repeat the test.

Jarvis parses the configuration for the initiation steps from the folder structure of the test case.
The folder structure contains a jarvisTestSuite.json configuration file, that allows various customizations to the tests, read about it under its own segment of the documentation.
The top-level folder can have any name, and the absolute path to it is one of the necessary parameters to run the application.
Every test case should have its own folder, but there isn't any naming convention enforced here.
In the case folder there must be a __'pre'__ and an __'assert'__ folder. 
The content of the 'pre' folder dictates what happens during the initiation phase, while the 'assert' folder controls the assertion step.
Within the 'pre' folder there are different valid folder structures depending on the environment.

#### BigQuery

##### 1. Option 

* folder with test cases (any name)
  * test_case_1
    * pre
        * _{context id}_.bql
    * assert
  * test_case_2
  * jarvisTestSuite.json
  
The file under the 'pre' folder must be named the same as the corresponding context's id.
Only the BQL and SQL file extensions are allowed.
One file can contain any number of statements as long as they can be executed in parallel and within the same context.

##### 2. Option 

It is also possible to upload the contents of a JSON file to BQ, but in this case the folder structure needs to be a bit different.

* folder with test cases (any name)
  * test_case_1
    * pre
        * _{context id}_
            * _{dataset name}_
              * _{table}_.json
    * assert
  * test_case_2
  * jarvisTestSuite.json
 
Here in the '_{context id}_' folder there can be other folders that have the name of BigQuery datasets.
The files in a '_{dataset name}_' folder are named as the target '_{table}_'.
The JSON content will be uploaded to the table and dataset specified here.
 
#### SQL
 
 Similar to the BigQuery folder structure, the following can be used to initiate MSSQL, MySQL or PostgreSQL environments. 
 
 * folder with test cases (any name)
   * test_case_1
     * pre
         * _{context id}_.sql
     * assert
   * test_case_2
   * jarvisTestSuite.json
   
The file under the 'pre' folder must be named the same as the corresponding context's id.
Only SQL file extension is allowed.
One file can contain any number of statements as long as they can be executed in parallel and within the same context.
 
#### SFTP
  
To prepare an SFTP server with pre-uploaded files, use the following folder structure. 
  
  * folder with test cases (any name)
    * test_case_1
      * pre
          * _{context id}_
            * files
      * assert
    * test_case_2
    * jarvisTestSuite.json
    
The folder under the 'pre' folder must be named the same as the corresponding context's id.
Within this folder there can be any number of files with any file extension.
The initiation step will copy these files to the SFTP server.
 
---
**NOTE**

 If the initiation is unnecessary it can be skipped by leaving the 'pre' folder empty.

---

### Execution Step

### Assertion Step

## jarvisTestSuite
//TODO when the finalized version is copied to Jarvis

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
    * jarvisTestSuite.json
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


jarvisTestSuite.json example
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
    * jarvisTestSuite.json
3. one or more .sql file somewhere in the project (jarvisTestSuite.json queryPath) to make Bq table modification(s).
    
Load to the BQ table and assert data are the same as it was at the bq view test.



In BqQuery test the jarvisTestSuite.json contains one or more BqQuery execution. contextType is always BqQuery, queryPath is the path feom the project folder to the .sql file.

jarvisTestSuite.json example
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

Make contexts.json file in test/resources to define parameters for the tests. LocalContext is for read local files, and the BigQuery context is for make the bigQuery job. The repositoryPath is the absolute path on the project folder. The test get the .sql file absolute path from the repositoryPath + jarvisTestSuite.json queryPath.
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
3. Set up the contexts.json with LocalContext and add execution to the jarvisTestSuite.json. LocalContext is for read local files.
4. Run IntegrationTestRunner.class

#### jarvisTestSuite.json

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