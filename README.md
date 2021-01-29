# jfr-streaming 
The jfr-streaming project provides a core library for configuring, starting, stopping, 
and reading [Java Flight Recording](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm#JFRUH170)
files from a JVM. The code does not depend on the `jdk.jfr`
module and will compile and run against JDK 8 or higher. It uses a connection to an MBean
server, which can be the platform MBean server, or a remote MBean server connected by
means of JMX. 

The goal of this project is a low-level library. Solving higher level problems, such
as managing JFR across multiple JVMs, is not a goal of this project. 

# Getting Started
This example illustrates some of the API. 
```java:    
    public static void main(String[] args) {
        MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            FlightRecorderConnection flightRecorderConnection = FlightRecorderConnection.connect(mBeanServer);
            RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
            RecordingConfiguration recordingConfiguration = RecordingConfiguration.PROFILE_CONFIGURATION;
            try (Recording recording = flightRecorderConnection.newRecording(recordingOptions, recordingConfiguration)) {
                recording.start();
                TimeUnit.SECONDS.sleep(10);
                recording.stop();
                recording.dump(Paths.get(System.getProperty("user.dir"), "recording.jfr").toString());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (InterruptedException ie) {

            }
        } catch (InstanceNotFoundException|IOException e) {
            e.printStackTrace();
        }
    }
```
Note that for Oracle JDK 8, it may be necessary to unlock the Java Flight Recorder 
commercial feature with the JVM arg `-XX:+UnlockCommercialFeatures -XX:+FlightRecorder`. 
Starting with JDK 8u262, Java Flight Recorder is available for all OpenJDK distributions.

# Build and Test
The build is vanilla Maven.
`mvn clean` - remove build artifacts
`mvn compile` - compile the source code
`mvn test` - run unit tests (this project uses TestNG)
`mvn package` - build the .jar file

# Contribute

This project welcomes contributions and suggestions. Most contributions require you to
agree to a Contributor License Agreement (CLA) declaring that you have the right to,
and actually do, grant us the rights to use your contribution. For details, visit
https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need
to provide a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the
instructions provided by the bot. You will only need to do this once across all repositories using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/)
or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
