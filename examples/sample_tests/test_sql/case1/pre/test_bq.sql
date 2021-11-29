DELETE FROM `{{project}}.tf_test.tf_test3` WHERE 1 = 1;

INSERT INTO `{{project}}.tf_test.tf_test3`
(user_id, counter)
VALUES
(1, 3);
