#hadoop-maven-plugin#

Overview
---------------

Maven Plugin for running Apache Hadoop jobs in pseudo-distributed-mode (default configuration is changeable with hmp.hadoopConf property).

Verified on Linux and Mac OS X / JDK 1.6.0_33 / Apache Hadoop 0.21.0 and 1.0.2.

Goals
---------------

    start - start NameNode, DataNode, JobTracker and TaskTracker
    copyFromLocal - copy file/directory from local file system to HDFS
    submitJob - submit job to Apache
    copyToLocal - copy file/directory form HDFS to local file system
    stop - stop daemons started by 'start' goal (necessary only if -Dhmp.autoShutdown=false been used)

>NOTE: maven-dependency-plugin & maven-jar-plugin are used to assembly job jar. See sample project for details.

Quickstart
---------------

Add following snippet to your pom.xml (adjust &lt;hadoopHome/&gt; if needed) and hit "mvn hadoop:start -Dhmp.autoShutdown=false"

            <plugin>
                <groupId>com.github.shyiko.hadoop-maven-plugin</groupId>
                <artifactId>hadoop-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <configuration>
                    <hadoopHome>${user.home}/hadoop-0.21.0</hadoopHome>
                </configuration>
            </plugin>

Sample
---------------

See [sample-maven-project](https://github.com/shyiko/hadoop-maven-plugin/tree/master/sample-maven-project).

License
---------------

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
