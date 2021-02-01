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
```java   
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
<br/>`mvn clean` - remove build artifacts
<br/>`mvn compile` - compile the source code
<br/>`mvn test` - run unit tests (this project uses TestNG)
<br/>`mvn package` - build the .jar file
