# Test environment setup for Jarvis

With this configurations terraform is able to provide the necessary infrastructure to run the integration tests of the Jarvis project.
It creates resources in BigQuery, Cloud SQL (MSSQL, MySQL, PostgreSQL) and GCE (SFTP server).

## Prerequisites
1. Create a GCP project.
2. Create a bucket for the backend ( '${project_name}_tf' ).
3. Set the name of the bucket in the main/main.tf file.
4. Set the name of the project in the main/variables.tf file.

## Dependencies

[gcloud cli](https://cloud.google.com/sdk/gcloud/)  
[sqlcmd](https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-setup-tools?view=sql-server-2017)
[mysql](https://docs.oracle.com/javacomponents/advanced-management-console-2/install-guide/mysql-database-installation-and-configuration-advanced-management-console.htm#JSAMI116)
[psql](https://www.postgresql.org/docs/11/tutorial-install.html)  

These will be used to execute a few commands locally, so do not forget to add them to PATH.

##Usage
Make sure you have all the dependencies mentioned above in the **Dependencies** installed.

1. From the current (`integration`) directory, navigate to the `main` folder. 
   In the command prompt do an init:  
   `terraform init`
   
2. If the environment already exists, you'll have to mark the previous instance as tainted, so that terraform destroys them on apply:  
   `terraform taint module.mssql.google_sql_database_instance.mssql_cloudsql`  
   `terraform taint module.mysql.google_sql_database_instance.mysql_cloudsql`  
   For the full list of resources use the `terraform state list` command and taint what you want recreate.
   Alternatively you can run a full cleanup with `terraform destroy`.

3. Run apply:   
   `terraform apply`  
   If everything seems okay, accept the suggested changes. 
   The whole apply process takes quite a bit of time, don't worry. 
    
4. After the process is finished, terraform outputs the attributes of the freshly created environment as a useable context JSON file to `src/test/resources/integration/integration-contexts.json`.
   Use this file in the integrations tests to connect to the resources.
   
5. When the development is finished or paused for a while, then run `terraform destroy` to reduce GCP costs.
   
 ---
 **NOTE**
 
 If you swap to a new GCP project, do not forget to create a service account that has BigQuery, Cloud SQL and GCE admin roles.
 Then add/change the new project and SA at the [GitHub Secrets](https://github.com/aliz-ai/jarvis/settings/secrets/actions).
 Without this step the GitHub Actions Maven CI will fail.
 ---
