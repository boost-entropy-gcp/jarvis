1. Set-up the BQ resources using the src/test/resources/sample_tests/terraform terraform module. Supply the name of the GCP project during the terraform apply
2. Create a context.json file using the src/test/resources/sample_tests/sample_contexts.json file as base. Fill in the project and the repository root parameters
3. Create a test.properties file in the src/test/resources/ folder using the sample.properties file. Fill in the the absolute path to the context file and the test folder
4. Run the ai.aliz.talendtestrunner.IntegrationTestRunner test to execute the integration test
