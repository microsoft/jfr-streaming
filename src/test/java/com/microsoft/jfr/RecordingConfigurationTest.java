package com.microsoft.jfr;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.management.RuntimeMBeanException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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

    @Test
    public void mapConfiguration() {

        Map<String, String> recordingConfigAsMap = new HashMap<>();
        recordingConfigAsMap.put("jdk.ObjectAllocationInNewTLAB#enabled", "true");
        recordingConfigAsMap.put("jdk.ObjectAllocationOutsideTLAB#enabled", "true");

        RecordingConfiguration recordingConfiguration = new RecordingConfiguration.MapConfiguration(recordingConfigAsMap);

        IItemCollection recordingContent = excecuteRecordingWithConfig(recordingConfiguration);
        assertTrue(containsEvent(recordingContent, "jdk.ObjectAllocationInNewTLAB"));
        assertTrue(containsEvent(recordingContent, "jdk.ObjectAllocationOutsideTLAB"));
    }

    private boolean containsEvent(IItemCollection recordingContent, String eventName) {
        for (IItemIterable iItemIterable : recordingContent) {
            for (IItem iItem : iItemIterable) {
                String currentEvent = iItem.getType().getIdentifier();
                if (currentEvent.equals(eventName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void executeRecording(String configFile) {
        RecordingConfiguration.JfcFileConfiguration configuration = new RecordingConfiguration.JfcFileConfiguration(RecordingConfigurationTest.class.getClassLoader().getResourceAsStream(configFile));
        excecuteRecordingWithConfig(configuration);
    }

    private IItemCollection excecuteRecordingWithConfig(RecordingConfiguration configuration) {
        Path dumpFile = null;
        try {
            dumpFile = Paths.get(System.getProperty("user.dir"), "testRecordingDump_dumped.jfr");
            Files.deleteIfExists(dumpFile);

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

            try {
                return JfrLoaderToolkit.loadEvents(dumpFile.toFile());
            } catch (CouldNotLoadRecordingException e) {
                fail("Unable to load JFR data: ", e);
            }

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
        return null;
    }

}
