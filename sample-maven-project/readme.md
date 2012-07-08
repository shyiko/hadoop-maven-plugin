#sample-maven-project#

Overview
---------------

Project consists of single Hadoop job - SortJob, which:

1. sorts sequence of integers (hadoop/input/sequence.txt file)
2. parses arguments using Apache Commons CLI (library is included inside job jar, see pom.xml)
3. uses HDFS directories for input/output
4. uses 1 mapper and 2 reducers

Usage
---------------

    # download and unpack Apache Hadoop 0.20.2+ from http://www.apache.org/dyn/closer.cgi/hadoop/common
    # no ssh or any other configuration is needed
    cd sample-maven-project
    mvn clean package hadoop:start hadoop:copyFromLocal hadoop:submitJob hadoop:copyToLocal -Dhmp.hadoopHome=<location of hadoop home directory> #1

\#1 basically means

1. prepare job jar
2. start hadoop
3. copy hadoop/input directory to HDFS
4. submit job and wait for its completion
5. copy sorted data from HDFS to hadoop/output
6. stop hadoop

\#1 can also be replaced with sequence of commands like shown below

    # change the value of hmp.hadoopHome property inside pom.xml (to eliminate necessity of passing it for each command below)
    mvn hadoop:start -Dhmp.autoShutdown=false #2
    mvn hadoop:copyFromLocal
    mvn hadoop:submitJob
    mvn hadoop:copyToLocal
    mvn hadoop:stop # required in order to stop started daemons at #2

>Troubleshooting: see /tmp/hmp.log