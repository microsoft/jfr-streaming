package com.microsoft.jfr;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class App 
{
    private static final ForkJoinPool threadPool = new ForkJoinPool();
    private static final int range = 100000;

    static class Fibonacci extends RecursiveAction {

        public BigInteger NthFibonacci(int n) {
            BigInteger[] fibonacciNumbers = new BigInteger[n+2];
            fibonacciNumbers[0] = BigInteger.ZERO;
            fibonacciNumbers[1] = BigInteger.ONE;
            for(int i = 2; i <= n; i++){
                fibonacciNumbers[i] = fibonacciNumbers[i-1].add(fibonacciNumbers[i-2]);
            }
            return fibonacciNumbers[n];
        }

        @Override
        protected void compute() {
            // Do some computation that heats up the CPU
            for (int i = 1; i<=range; i++){
                System.out.println( i + "th Fibonacci number: " + NthFibonacci(i).toString());
            }
        }
    }

    public static void main( String[] args )
    {
        // Create a connection to the mBean server
        MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            // In order to upload a JFR file from a remote JVM, we need to create a FlightRecorderConnection
            FlightRecorderConnection flightRecorderConnection = FlightRecorderConnection.connect(mBeanServer);
            RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
            RecordingConfiguration recordingConfiguration = RecordingConfiguration.PROFILE_CONFIGURATION;

            // Try creating a new recording instance. This doesn't start recording yet.
            try (Recording recording = flightRecorderConnection.newRecording(recordingOptions, recordingConfiguration)) {
                // Start recording
                recording.start();

                threadPool.invoke(new Fibonacci());

                // Stop recording
                recording.stop();

                // Save the recording.jfr file that can be used in JDK Mission control
                recording.dump(Paths.get(System.getProperty("user.dir"), "recording.jfr").toString());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } catch (InstanceNotFoundException | IOException | JfrStreamingException e) {
            e.printStackTrace();
        }
    }
}
