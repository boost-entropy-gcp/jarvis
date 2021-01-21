# Test environment setup for Jarvis

With this configurations terraform is able to provide the necessary infrastructure to run the integration tests of the Jarvis project.
It creates resources in BigQuery, Cloud SQL (MSSQL, MySQL, PostgreSQL) and GCE.

## Prerequisites
1. Create a GCP project.
2. Create a bucket for the backend ( '${project_name}_tf' ).
3. Set the name of the bucket in the environments/dev/main.tf file.
4. Set the name of the project in the environments/dev/main.tf file.

## Dependencies

[gcloud cli](https://cloud.google.com/sdk/gcloud/)  
[sqlcmd](https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-setup-tools?view=sql-server-2017)
[mysql](https://docs.oracle.com/javacomponents/advanced-management-console-2/install-guide/mysql-database-installation-and-configuration-advanced-management-console.htm#JSAMI116)
[psql]()  

These will be used to execute a few commands locally, so do not forget to add them to PATH.

##Usage
Make sure you have all the dependencies mentioned above in the **Dependencies** installed.

1. From the current (terraform) directory, navigate to the `main` folder. 
   In a command prompt do an init:  
   `terraform init`
   
2. If the environment already exists, you'll have to mark the previous instance as tainted, so that terraform destroys them on apply:  
   `terraform taint module.mssql.google_sql_database_instance.mssql_cloudsql`  
   `terraform taint module.mysql.google_sql_database_instance.mysql_cloudsql`  
   For the full list of resources use the `terraform state list` command and taint what you want recreate.

2. Run apply:   
   `terraform apply`  
   If everything seems okay, accept the suggested changes. 
   The whole apply process takes quite a bit of time, don't worry. 
    
3. After the process is finished, terraform prints the username, password and the IP address (host) of the freshly created environment.
   (Should not change at recreation, if you followed these steps.) 
   Use them to connect to the database in the integrations tests.
   
 ---
 **NOTE**
 
 If you swap to a new GCP project, do not forget to create a service account that has BigQuery, Cloud SQL and GCE admin roles.
 Then add/change the new project and SA at the [GitHub Secrets](https://github.com/aliz-ai/jarvis/settings/secrets/actions).
 Without this step the GitHub Actions Maven CI will fail.
 ---
