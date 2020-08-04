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
