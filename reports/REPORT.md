# Database: Replication report

Testing the behavior of a database with replication.

## Setting up databases and replication

Starting the master and two slaves.

Preparing the master database:

```sql
CREATE USER IF NOT EXISTS 'slave_one'@'%' IDENTIFIED BY 'slave';
CREATE USER IF NOT EXISTS 'slave_two'@'%' IDENTIFIED BY 'slave';
GRANT REPLICATION SLAVE ON *.* TO 'slave_one'@'%';
GRANT REPLICATION SLAVE ON *.* TO 'slave_two'@'%';
FLUSH PRIVILEGES;
```

#### Master status:

```sql
SHOW MASTER STATUS;
```

```
File=mysql-bin.000003
Position=73719
```

Preparing first slave:

```sql
STOP SLAVE;
DROP TABLE IF EXISTS test;
CHANGE MASTER TO MASTER_HOST='mysql-master',
MASTER_USER='slave_one',
MASTER_PASSWORD='slave',
MASTER_LOG_FILE='mysql-bin.000003',
MASTER_LOG_POS=73719;
START SLAVE;
```

Preparing second slave:

```sql
STOP SLAVE;
DROP TABLE IF EXISTS test;
CHANGE MASTER TO MASTER_HOST='mysql-master',
MASTER_USER='slave_two',
MASTER_PASSWORD='slave',
MASTER_LOG_FILE='mysql-bin.000003',
MASTER_LOG_POS=73719;
START SLAVE;
```

Inserting data into the master:

```sql
CREATE TABLE IF NOT EXISTS test.users (
id INT NOT NULL AUTO_INCREMENT,
name VARCHAR(45) NOT NULL,
PRIMARY KEY (id));;
TRUNCATE TABLE test.users;
```

#### Starting daemon to constantly populate master:

```sql
INSERT INTO test.users (name) VALUES ('${UUID.randomUUID()}');
```

## Checking replication

#### Checking slave statuses:

Slave one:

```
Auto_Position=0
Connect_Retry=60
Exec_Master_Log_Pos=82520
Get_master_public_key=0
Last_Errno=0
Last_IO_Errno=0
Last_SQL_Errno=0
Master_Host=mysql-master
Master_Info_File=mysql.slave_master_info
Master_Log_File=mysql-bin.000003
Master_Port=3306
Master_Retry_Count=86400
Master_SSL_Allowed=No
Master_SSL_Verify_Server_Cert=No
Master_Server_Id=1
Master_UUID=65256716-e2ea-11ed-9755-0242ac120002
Master_User=slave_one
Read_Master_Log_Pos=83162
Relay_Log_File=mysql-relay-bin.000002
Relay_Log_Pos=9127
Relay_Log_Space=9979
Relay_Master_Log_File=mysql-bin.000003
SQL_Delay=0
SQL_Remaining_Delay=null
Seconds_Behind_Master=0
Skip_Counter=0
Slave_IO_Running=Yes
Slave_IO_State=Waiting for source to send event
Slave_SQL_Running=Yes
Slave_SQL_Running_State=Replica has read all relay log; waiting for more updates
Until_Condition=None
Until_Log_Pos=0
```

Slave two:

```
Auto_Position=0
Connect_Retry=60
Exec_Master_Log_Pos=82841
Get_master_public_key=0
Last_Errno=0
Last_IO_Errno=0
Last_SQL_Errno=0
Master_Host=mysql-master
Master_Info_File=mysql.slave_master_info
Master_Log_File=mysql-bin.000003
Master_Port=3306
Master_Retry_Count=86400
Master_SSL_Allowed=No
Master_SSL_Verify_Server_Cert=No
Master_Server_Id=1
Master_UUID=65256716-e2ea-11ed-9755-0242ac120002
Master_User=slave_two
Read_Master_Log_Pos=83162
Relay_Log_File=mysql-relay-bin.000002
Relay_Log_Pos=9448
Relay_Log_Space=9979
Relay_Master_Log_File=mysql-bin.000003
SQL_Delay=0
SQL_Remaining_Delay=null
Seconds_Behind_Master=0
Skip_Counter=0
Slave_IO_Running=Yes
Slave_IO_State=Waiting for source to send event
Slave_SQL_Running=Yes
Slave_SQL_Running_State=Replica has read all relay log; waiting for more updates
Until_Condition=None
Until_Log_Pos=0
```

Checking slave data:

Master count: `28`

Slave one count: `28`

Slave two count: `28`

`Slave data is in sync`

## Turning off one of the slaves

#### Stopping slave two

Checking slave data:

Master count: `83`

Slave one count: `83`

`Slave data is in sync`

#### Starting slave two again

Checking slave data:

Master count: `170`

Slave one count: `170`

Slave two count: `170`

`Slave data is in sync`

## Trying to remove column on the read-write slave

### On the read-write slave

Removing column on slave:

```sql
ALTER TABLE test.users DROP COLUMN name;
```

Exception: `Can't DROP 'name'; check that column/key exists`

### On the read-only slave

Removing column on slave:

```sql
ALTER TABLE test.users DROP COLUMN name;
```

Exception: `The MySQL server is running with the --read-only option so it cannot execute this statement`

### Final Replication status

Checking slave data:

Master count: `171`

Slave one count: `171`

Slave two count: `171`

`Slave data is in sync`

