package com.microsoft.jfr;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;

/**
 * This sample is meant to show the basic usage of the jfr-streaming library
 */
public class Main {

    public static void main( String[] args ) {
        // A mBean server is needed to connection to the FlightRecorder via JMX
        MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            // In order to upload a JFR file from a remote JVM, we need to create a FlightRecorderConnection
            FlightRecorderConnection flightRecorderConnection = FlightRecorderConnection.connect(mBeanServer);
            RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
            RecordingConfiguration recordingConfiguration = RecordingConfiguration.PROFILE_CONFIGURATION;

            try {
                // Try creating a new recording instance. This doesn't start recording yet.
                Recording recording = flightRecorderConnection.newRecording(recordingOptions, recordingConfiguration);

                recording.start();

                // Foo does some busy work
                new LoadGenerator(args).generateLoad();

                recording.stop();

                // Save the recording.jfr file that can be used in JDK Mission control
                recording.dump(Paths.get(System.getProperty("user.dir"), "recording.jfr").toString());
            } catch (IOException ioe) {
                // IOException can occur when we try to start/stop recording
                ioe.printStackTrace();
            }
        } catch (InstanceNotFoundException | IOException | JfrStreamingException e) {
            // If the jfr-streaming library throws an `InstanceNotFoundException`, you may need to enable commercial
            // features by providing `-XX:+UnlockCommercialFeatures` on the command line.
            //
            // If `IOException` is thrown, there might be a communication problem when talking to the MBean server.
            //
            // If it throws an `JfrStreamingException`, there might be a bug in the jfr-streaming library code
            e.printStackTrace();
        }
    }
}
