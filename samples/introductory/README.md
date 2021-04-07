# jfr-streaming Sample app - fibonacci numbers

This sample uses jfr-streaming to capture a flight recording file. The sample creates the file `recording.jfr` which can be opened in JDK Mission control to visualize the stats.

This app prints fibonacci numbers using multi threading, so that all the CPU cores get used.

### Execute

To execute this single module, first we need to do:
```shell
$ cd samples/introductory
$ mvn clean compile
```
Once the build is done, we can run the app as many times as we want:
```shell
$ mvn exec:java
```
This command uses default value for `range` from the `pom.xml` file. We can configure that value by passing it as an argument to this command `-Dexec.arguments="1000"`