package com.microsoft.jfr.dcmd;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.microsoft.jfr.JfrStreamingException;
import com.microsoft.jfr.RecordingConfiguration;
import com.microsoft.jfr.RecordingOptions;

import org.junit.Test;
import org.testng.Assert;

public class FlightRecorderDiagnosticCommandConnectionTest {

    public MBeanServerConnection mockMbeanServer(ObjectName objectName, String vmCheckCommercialFeaturesResponse) throws Exception {
        MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
        when(mBeanServerConnection.invoke(objectName, "vmCheckCommercialFeatures", null, null)).thenReturn(vmCheckCommercialFeaturesResponse);
        return mBeanServerConnection;
    }
    
    @Test
    public void assertCommercialFeaturesUnlocked() throws Exception {
        ObjectName objectName = mock(ObjectName.class);
        MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "unlocked");   
        FlightRecorderDiagnosticCommandConnection.assertCommercialFeaturesUnlocked(mBeanServerConnection, objectName);
    }

    @Test(expected = JfrStreamingException.class)
    public void assertCommercialFeaturesLockedThrows() throws Exception {
        ObjectName objectName = mock(ObjectName.class);
        MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "locked");
        FlightRecorderDiagnosticCommandConnection.assertCommercialFeaturesUnlocked(mBeanServerConnection, objectName);
    }

    private FlightRecorderDiagnosticCommandConnection createconnection() throws Exception {
        ObjectName objectName = mock(ObjectName.class);
        MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "locked");
        return new FlightRecorderDiagnosticCommandConnection(mBeanServerConnection, objectName);
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void closeRecording() throws Exception {
        createconnection().closeRecording(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetStream() throws Exception {
        createconnection().getStream(1L, null, null, 0L);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCloneRecording() throws Exception {
        createconnection().cloneRecording(1, false);
    }

    @Test
    public void startRecordingParsesIdCorrectly() throws Exception {
        ObjectName objectName = mock(ObjectName.class);
        MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "unlocked");
        when(mBeanServerConnection.invoke(any(ObjectName.class), anyString(), any(Object[].class),
                any(String[].class))).thenReturn("Started recording 99. ");
        FlightRecorderDiagnosticCommandConnection connection = new FlightRecorderDiagnosticCommandConnection(mBeanServerConnection, objectName);
        long id = connection.startRecording(new RecordingOptions.Builder().build(), RecordingConfiguration.PROFILE_CONFIGURATION);
        Assert.assertEquals(id, 99);
    }
}
