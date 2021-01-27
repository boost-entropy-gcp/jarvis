# Test environment setup for Jarvis

With this configurations Terraform is able to provide the necessary infrastructure to run the integration tests of the Jarvis project.
It creates resources in BigQuery, Cloud SQL (MSSQL, MySQL, PostgreSQL) and GCE (SFTP server, still TODO).

## Prerequisites
1. Create a GCP project.
2. Create a bucket for the backend ( '${project_name}_tf' ).
3. Create a service account and add BigQuery and Cloud SQL admin roles.
4. Set the name of the bucket in the main/main.tf file.
5. Set the name of the project and service account in the main/variables.tf file.

## Dependencies

[gcloud cli](https://cloud.google.com/sdk/gcloud/)  
[sqlcmd](https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-setup-tools?view=sql-server-2017)  
[mysql](https://docs.oracle.com/javacomponents/advanced-management-console-2/install-guide/mysql-database-installation-and-configuration-advanced-management-console.htm#JSAMI116)  
[psql](https://www.postgresql.org/docs/11/tutorial-install.html)   
[kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) (only if SFTP gets done)

These will be used to execute a few commands locally, so do not forget to add them to PATH.

## Usage
Make sure you have all the dependencies mentioned above in the **Dependencies** installed.

1. From the current (`integration`) directory, navigate to the `main` folder. 
   In the command prompt do an init:  
   `terraform init`
   
2. Run apply:   
   `terraform apply`  
   If everything seems okay, accept the suggested changes. 
   The whole apply process takes quite a bit of time, don't worry. 
   
   If resources already exist and `terraform apply` does not detect changes correctly, then you can run a full cleanup with `terraform destroy`.
   If something is left behind, you'll have to mark the previous instance as tainted, so that Terraform destroys them on apply:  
   `terraform taint module.mssql.google_sql_database_instance.mssql_cloudsql`  
   `terraform taint module.mysql.google_sql_database_instance.mysql_cloudsql`  
   For the full list of resources use the `terraform state list` command and taint what you want to recreate.
    
4. After the process is finished, Terraform outputs the attributes of the freshly created environment as context JSON files to `src/test/resources/integration`.
   The integrations tests use these files to connect to the resources.
   Another output file is the `.github/workflows/db_authorization.json`, which is used by the GitHub Actions CI to temporarily grant the runner access to the databases and execute the integration tests. 
   
5. When the development is finished or paused for a longer period, then run `terraform destroy` to reduce GCP costs.
   
 ---
 **NOTE**
 
 If you swap to a new GCP project, do not forget to create a service account that has BigQuery and Cloud SQL admin roles.
 Then add/change the new project and SA at the [GitHub Secrets](https://github.com/aliz-ai/jarvis/settings/secrets/actions).
 Without this step the GitHub Actions Maven CI will fail.
 ---
