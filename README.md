# Highload Software Architecture 8 Lesson 20 Homework

Database: Replication
---

## Test project setup

The demo is written in Kotlin and uses Gradle as a build tool.
MysSQL replication setup is defined in the [docker-compose.yaml](docker-compose.yaml) file.

The `com.example.DemoKt.runDemo` function is used to run all the scenarios.
It also writes the [REPORT.md](reports/REPORT.md) file as it executes the scenarios.

## How to build and run

Build and run demo application (Requires Java 11+)

```shell script
./gradlew run
```

## Conclusion

The investigation aimed to test the behavior of a database with replication by setting up a master database and two slave databases.
Throughout the testing, the replication was consistently working as expected, with the slave data remaining in sync with the master. This
was observed during various operations, including when one of the slaves was turned off and then back on.

Furthermore, the investigation demonstrated the difference between the read-write and read-only slaves. While the read-write slave allowed
the removal of a column without any issues, the read-only slave raised an exception, indicating that it cannot execute such a statement.

In conclusion, the replication setup proved to be stable and reliable, with the slave databases maintaining synchronization with the master
database during different scenarios. Additionally, the read-only and read-write slaves behaved as expected, adhering to their respective
limitations.
