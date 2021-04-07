# jfr-streaming Samples

This directory contains sample maven apps that uses `jfr-streaming` API. We can run each sample app by running maven from this directory directly.

### Execute

First build all the sample apps:
```shell
$ cd samples
$ mvn clean compile
```
Once the build is done, we can run an app by specifying the module name with this flag: `-pl sample`, where `sample` is the module name
```shell
$ mvn exec:java -pl introductory
```
This command uses default value for `range` from the `pom.xml` file. We can configure that value by passing it as an argument to this command `-Dexec.arguments="1000"`