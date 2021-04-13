# Microsoft JFR Streaming

The `jfr-streaming` project provides a core library for configuring, starting, stopping, 
and reading [Java Flight Recording](https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm#JFRUH170)
files from a JVM. The code does not depend on the `jdk.jfr`
module and will compile and run against JDK 8 or higher. It uses a connection to an MBean
server, which can be the platform MBean server, or a remote MBean server connected by
means of JMX. 

The goal of this project is a low-level library. Solving higher level problems, such
as managing JFR across multiple JVMs, is not a goal of this project. 

## Getting Started

### Maven Coordinates

```xml
<dependency>
  <groupId>com.microsoft.jfr</groupId>
  <artifactId>jfr-streaming</artifactId>
  <version>1.1.0</version>
</dependency>
```

### Example

This example illustrates some of the API. 

```java   
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.microsoft.jfr:jfr-streaming:1.1.0
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import javax.management.*;
import com.microsoft.jfr.*;

public class Sample {
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
                System.out.println("JFR recording ready: recording.jfr");
            }
        } catch (InstanceNotFoundException | IOException | JfrStreamingException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

You can run the code above with [jbang](https://www.jbang.dev):

1. Install jbang.
1. Save the code above in a local `Sample.java` file, or [download directly](https://raw.githubusercontent.com/microsoft/jfr-streaming/main/samples/jbang/Sample.java).
1. Run the code: `jbang Sample.java`

### Note on Oracle JDK 8

For Oracle JDK 8, it may be necessary to unlock the Java Flight Recorder 
commercial feature with the JVM arg `-XX:+UnlockCommercialFeatures -XX:+FlightRecorder`.
Starting with JDK 8u262, Java Flight Recorder is available for all OpenJDK distributions.

## Build and Test

The build is vanilla Maven.

<br/>`mvn clean` - remove build artifacts
<br/>`mvn compile` - compile the source code
<br/>`mvn test` - run unit tests (this project uses TestNG)
<br/>`mvn package` - build the .jar file

## Contributing

This project welcomes contributions and suggestions. Most contributions require you to agree to a Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For details, view [Microsoft's CLA](https://cla.microsoft.com).

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions provided by the bot. You will only need to do this once across all repositories using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## License

Microsoft JFR Streaming Library is licensed under the [MIT](https://github.com/microsoft/jfr-streaming/blob/master/LICENSE) license.
