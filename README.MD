#Test runner

###Test BQ view

######Folder system:

1. test (= CONFIG_PATH)
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
        * testSuite.json     
    * test_case2
    
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

.sql example:
```
DELETE FROM `{project}.{dataset}.{table}` WHERE 1=1;

INSERT INTO `{project}.{dataset}.{table}` (user_id, age)
VALUES (1, 1);
```

testSuite.json example
```
{
  "caseAutoDetect": true
}
```

######contexts json

context.json example

contexts.json = CONTEXT_PATH
```
[
  {
    "id": {id},
    "type": "BigQuery",
    "parameters": {
      "project": {project name}
    }
  }
]
```

#####To run test for view:

1. Set up the folder system with the files
2. In IntegrationTestRunner.class set the CONTEXTS_PATH and the CONFIG_PATH
3. Run IntegrationTestRunner.class
