# jfr-streaming introductory sample

This sample uses jfr-streaming to capture a flight recording file. The sample creates the file `recording.jfr` which 
can be opened in JDK Mission Control to visualize the stats.

### Execute

To build the sample from the 'introductory' directory:
```shell
$ mvn clean compile
```
To run the sample:
```shell
$ mvn exec:java
```
The sample simulates load by generating numbers of a Fibonacci sequence. The number of 
values to generate can be specified by passing it as an argument to this command, for
example `-Dexec.arguments="1000"`