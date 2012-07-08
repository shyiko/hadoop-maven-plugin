#hadoop-maven-plugin#

Overview
---------------

Maven Plugin for running Apache Hadoop jobs in pseudo-distributed-mode (default configuration changeable with hmp.hadoopConf property).

Goals
---------------

    start - start NameNode, DataNode, JobTracker and TaskTracker
    copyFromLocal - copy file/directory from local file system to HDFS
    submitJob - submit job to Apache
    copyToLocal - copy file/directory form HDFS to local file system
    stop - stop daemons started by 'start' goal (necessary only if -Dhmp.autoShutdown=false been used)

Usage
---------------

See [sample-maven-project].

License
---------------

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
