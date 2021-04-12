package com.microsoft.jfr;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.management.RuntimeMBeanException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class RecordingConfigurationTest {

    FlightRecorderConnection flightRecorderConnection = null;

    @BeforeTest
    public void setup() {
        flightRecorderConnection = RecordingTest.getFlightRecorderConnection();
    }

    @AfterTest
    public static void tearDown() {
        RecordingTest.tearDown();
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void nullConfigThrows() {
        new RecordingConfiguration.JfcFileConfiguration(null);
    }

    @Test(expectedExceptions = RuntimeMBeanException.class)
    public void brokenJfcConfigFileThrowsError() {
        executeRecording("brokenJfcFile.jfc");
    }

    @Test
    public void jfcFileFromInputStreamCanBeRead() {
        executeRecording("sampleJfcFile.jfc");
    }

    private void executeRecording(String configFile) {
        Path dumpFile = null;
        try {
            dumpFile = Paths.get(System.getProperty("user.dir"), "testRecordingDump_dumped.jfr");
            Files.deleteIfExists(dumpFile);

            RecordingConfiguration.JfcFileConfiguration configuration = new RecordingConfiguration.JfcFileConfiguration(RecordingConfigurationTest.class.getClassLoader().getResourceAsStream(configFile));
            Recording recording = flightRecorderConnection.newRecording(null, configuration);
            long id = recording.start();
            Instant now = Instant.now();
            Instant then = now.plusSeconds(1);
            while (Instant.now().compareTo(then) < 0) {
                RecordingTest.fib(Short.MAX_VALUE); // do something
            }
            recording.stop();
            recording.dump(dumpFile.toString());
            assertTrue(Files.exists(dumpFile));
        } catch (IllegalArgumentException badData) {
            fail("Issue in test data: " + badData.getMessage());
        } catch (IOException ioe) {
            // possible that this can be thrown, but should not happen in this context
            fail("IOException not expected: ", ioe);
        } catch (JfrStreamingException badBean) {
            fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
        } finally {
            if (dumpFile != null) {
                try {
                    Files.deleteIfExists(dumpFile);
                } catch (IOException ignore) {
                }
            }
        }
    }

}
