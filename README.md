## Neo4j Remote Graph Downloader

This repository contains the source code of a simple implementation that downloads a remote Neo4j graph database to local machine. Download jar file [here](https://www.dropbox.com/s/3ct5wznok09lvk5/dumper.jar?dl=1).

Download a remote database at `bolt//neo4j.database` to local directory `outFile`:  
```shell script
java -jar dumper.jar -uri bolt://neo4j.database -u username -p password -t outFile
```  

Print help:
```shell script
java -jar dumper.jar -h
```
