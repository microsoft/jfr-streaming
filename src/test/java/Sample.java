///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.microsoft.jfr:jfr-streaming:1.0.0

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
            }
        } catch (InstanceNotFoundException | IOException | JfrStreamingException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
