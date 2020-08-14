#Test runner

###Test BQ view

######Folder system:
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

######contexts json

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

#####To run test for view:

1. Set up the folder system with the files, pay attention of naming the folders and files, the json format and data in the json files.
2. In IntegrationTestRunner.class set the CONTEXTS_PATH and the CONFIG_PATH. CONTEXTS_PATH is the absolute path of the contexts.json. CONFIG_PATH is the absolute path of the folder which has the test cases.
3. Run IntegrationTestRunner.class


###Test BQ query

######Folder system
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
           "contextType": "BqQuery",
           "queryPath": "\\src\\test\\resources\\test.sql", (example)
           "executionContext": "{context id}"
       }
     ]
}
```

######contexts json

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

#####To run test for BqQuery:

1. Set up the folder system with the files, pay attention of naming the folders and files, the json format and data in the json files.
2. In IntegrationTestRunner.class set the CONTEXTS_PATH and the CONFIG_PATH. CONTEXTS_PATH is the absolute path of the contexts.json. CONFIG_PATH is the absolute path of the folder which has the test cases.
3. Set up the contexts.json with LocalContext and add execution to the testSuit.json. LocalContext is for read local files.
4. Run IntegrationTestRunner.class

####testSuits.json

1. caseAutoDetect
    * boolean
        * If it's true the test will make initActionConfigs and assertActionConfigs.
        
        
######executions:

1. execution
    * List of
        * executionType (type of execution. _BigQuery, Airflow, Talend, NoOps_)
        * queryPath (path of the file for do the execution)
        * executionContext (context id)
2. 
######defaultProperties:

1. assert.excludePreviouslyInsertedRows
    * boolean
        * Deafult value is false. If it's true it puts "WHERE {tableName}_INSERTED_BY != 'test_init'" to the query for assert.
2. assert.assertKeyColumns
    * list -> default {tableName}_BID, {tableName}_VALID_FROM
        *
3. init.{contextId}.noMetadatAddition
    * boolean
        * Default value is true. If it's false the test add a {tableName}_INSERTED_BY field with "test_init" value to the table which load by json file.