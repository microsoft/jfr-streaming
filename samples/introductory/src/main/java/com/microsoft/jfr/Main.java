// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.package com.microsoft.jfr;
package com.microsoft.jfr;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;

/**
 * This sample is meant to show the basic usage of the jfr-streaming library. This library provides access
 * to most of the FlightRecorderMXBean API. There are a few steps to using the library.
 * <p>
 * First, an MBeanServerConnection is needed. The MBeanServerConnection can be to a local or
 * remote MBean server. For simplicity, this sample makes a connection to the MBean server of
 * the JVM that is running the sample. The library only uses the MBeanServerConnection. Creating,
 * managing and closing the MBeanServerConnection is the responsibility of the caller.
 * <p>
 * Next, a connection to the FlightRecorderMXBean is made using the
 * {@code com.microsoft.jfr.FlightRecorderConnection#connect(javax.management.MBeanServerConnection)}
 * method. This call returns a {@code com.microsoft.jfr.FlightRecorderConnection} which can be used
 * to create Java flight recordings. Only one FlightRecorderConnection is needed for a given
 * MBeanServerConnection.
 * <p>
 * To create a flight recording, call the method
 * {@code com.microsoft.jfr.FlightRecorderConnection#newRecording(com.microsoft.jfr.RecordingOptions, com.microsoft.jfr.RecordingConfiguration)}.
 * This method takes two parameters. The first parameter specifies the options that control the
 * flight recording, such as maximum recording size. The second parameter configures what events are
 * recorded, typically the 'default' or 'profiling' configurations that are built-in to the JVM.
 * <p>
 * The {@code newRecording} method returns a {@code com.microsoft.jfr.Recording} object that is used to
 * start, stop and stream a recording file. The {@code Recording} can be used repeatedly provided the
 * {@code Recording} has not been closed.
 * </p>
 */
public class Main {

    /**
     * An integer value can be passed as an argument on the command line. This integer
     * argument specifies the end of a range of Fibonacci numbers to generate. If no
     * argument is given, a default value (1000) is used. The larger the number, the more
     * load will be placed on the system and the more events that will be generated for the
     * flight recording. But a larger number also means a longer runtime.
     * @param args
     */
    public static void main( String[] args ) {
        // This sample uses the local MBean server, but this could also be a remote MBean server.
        MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            // In order to upload a JFR file, we need to create a FlightRecorderConnection.
            // This gives us a connection the the FlightRecorderMXBean on the MBean server.
            FlightRecorderConnection flightRecorderConnection = FlightRecorderConnection.connect(mBeanServer);

            // To create a recording, we need recording options, and a recording configuration.
            // This sample uses the 'profile' configuration. Refer to the section on
            // "Flight Recorder Configurations" in the Flight Recorder API Programmer's Guide
            // for more information.
            RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
            RecordingConfiguration recordingConfiguration = RecordingConfiguration.PROFILE_CONFIGURATION;

            // Note that a Recording is AutoCloseable, so we can use it in a try-with-resources block.
            try (Recording recording = flightRecorderConnection.newRecording(recordingOptions, recordingConfiguration)) {

                // To create a flight recording, the Recording has to be started. Once the
                // recording is started, flight recording events are collected by the JVM.
                recording.start();

                // LoadGenerator does some busy work to drive up CPU usage.
                new LoadGenerator(args).generateLoad();

                // It isn't always necessary to stop the recording. Check the {@code com.microsoft.jfr.Recording} API.
                recording.stop();

                // The dump method writes the file to the given path on the host where the JVM is running.
                // The 'getStream' method is most useful for reading a flight recording file from a remote JVM.
                // This sample saves the flight recording to 'recording.jfr', which can be used in JDK Mission control
                recording.dump(Paths.get(System.getProperty("user.dir"), "recording.jfr").toString());
            } catch (IOException ioe) {
                // IOException can occur when we try to start/stop recording
                ioe.printStackTrace();
            }
        } catch (InstanceNotFoundException | IOException | JfrStreamingException e) {
            // `InstanceNotFoundException` means that you may need to enable commercial
            // features by providing `-XX:+UnlockCommercialFeatures` on the command line.
            // `IOException` means there is a communication problem when talking to the MBean server.
            // `JfrStreamingException` wraps an exception that may be thrown by the MBean server or from
            // the FlightRecorderMXBean.
            e.printStackTrace();
        }
    }
}
