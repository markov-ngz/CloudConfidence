USE rest ;

-- create ns
CREATE DATABASE rest.sandbox ;

USE rest.sandbox;


create table rest.sandbox.my_table (
   k int,
   v string
) tblproperties (
    'primary-key' = 'k'
);


INSERT INTO rest.sandbox.my_table(k,v) VALUES (1,'a') , (2,'b') ;

SELECT * FROM rest.sandbox.my_table ;

ALTER TABLE rest.sandbox.my_table  SET TBLPROPERTIES (
    'write.wap.enabled'='true'
    );


ALTER TABLE rest.sandbox.my_table CREATE BRANCH `test_branch_123` ;

SELECT * FROM rest.sandbox.my_table.branch_test_branch_123; -- fails here

INSERT INTO rest.sandbox.my_table.test_branch_123 VALUES (3,'a') , (4,'b') ;

CALL rest.system.fast_forward('sandbox.my_table', 'main', 'test_branch_123');

ALTER TABLE rest.sandbox.my_table DROP BRANCH IF EXISTS test_branch_123 ;



DELETE FROM rest.sandbox.my_table ;
DROP TABLE rest.sandbox.my_table ;