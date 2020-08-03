SELECT first_name, last_name, age FROM `${project}.tf_test.tf_test1`
  LEFT JOIN `${project}.tf_test.tf_test2` ON id = user_id