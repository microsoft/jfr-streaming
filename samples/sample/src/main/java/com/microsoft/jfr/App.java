package com.microsoft.jfr;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;

public class App {
    private static ArrayList<BigInteger> cache = new ArrayList<>();
    private static int lastFibonacciIndex = -1;
    private static int range = 100;

    public static BigInteger NthFibonacci(int n) {
        if (lastFibonacciIndex >= n) return cache.get(n);
        for(int i = lastFibonacciIndex + 1; i <= n; i++){
            cache.set(i, cache.get(i - 1).add(cache.get(i - 2)));
        }
        lastFibonacciIndex = n;
        return cache.get(n);
    }

    public static void main( String[] args ) {
        try {
            range = Integer.parseInt(args[0]);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            // IndexOutOfBoundsException happens when args is empty, which means no argument was passed
            // NumberFormatException happens when args[0] is not a number
            // In both cases we use the default value for `range` as 100
        }
        cache = new ArrayList<>(Collections.nCopies(range + 1, BigInteger.ZERO));

        // A mBean server is needed to connection to the FlightRecorder via JMX
        MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();

        IntStream stream = IntStream.range(0, range);
        cache.set(0, BigInteger.ZERO);
        cache.set(1, BigInteger.ONE);
        lastFibonacciIndex = 1;

        try {
            // In order to upload a JFR file from a remote JVM, we need to create a FlightRecorderConnection
            FlightRecorderConnection flightRecorderConnection = FlightRecorderConnection.connect(mBeanServer);
            RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
            RecordingConfiguration recordingConfiguration = RecordingConfiguration.PROFILE_CONFIGURATION;

            try {
                // Try creating a new recording instance. This doesn't start recording yet.
                Recording recording = flightRecorderConnection.newRecording(recordingOptions, recordingConfiguration);

                recording.start();

                // Each value in `stream` gets its own thread to run
                stream.parallel().forEach(i -> System.out.println( i + "th Fibonacci number: " + NthFibonacci(i).toString()));

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

            // If `IOException` is thrown, there might be a communication problem when talking to the MBean server.

            // If it throws an `JfrStreamingException`, there might be a bug in the jfr-streaming library code
            e.printStackTrace();
        }
    }
}
