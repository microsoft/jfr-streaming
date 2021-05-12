package com.microsoft.jfr.flightRecorderConnections;

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

public class FlightRecorderConnectionJava8Test {

    public MBeanServerConnection mockMbeanServer(ObjectName objectName, String vmCheckCommercialFeaturesResponse) throws Exception {
        MBeanServerConnection mBeanServerConnection = mock(MBeanServerConnection.class);
        when(mBeanServerConnection.invoke(objectName, "vmCheckCommercialFeatures", null, null)).thenReturn(vmCheckCommercialFeaturesResponse);
        return mBeanServerConnection;
    }
    
    @Test
    public void assertCommercialFeaturesUnlocked() throws Exception {
        ObjectName objectName = mock(ObjectName.class);
        MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "unlocked");   
        FlightRecorderConnectionJava8.assertCommercialFeaturesUnlocked(mBeanServerConnection, objectName);
    }

    @Test(expected = JfrStreamingException.class)
    public void assertCommercialFeaturesLockedThrows() throws Exception {
        ObjectName objectName = mock(ObjectName.class);
        MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "locked");
        FlightRecorderConnectionJava8.assertCommercialFeaturesUnlocked(mBeanServerConnection, objectName);
    }

    private FlightRecorderConnectionJava8 createconnection() throws Exception {
        ObjectName objectName = mock(ObjectName.class);
        MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "locked");
        return new FlightRecorderConnectionJava8(mBeanServerConnection, objectName);
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
    public void startRecordingThrowsJfrStreamingException() throws Exception {
        ObjectName objectName = mock(ObjectName.class);
        MBeanServerConnection mBeanServerConnection = mockMbeanServer(objectName, "unlocked");
        when(mBeanServerConnection.invoke(any(ObjectName.class), anyString(), any(Object[].class),
                any(String[].class))).thenReturn("Started recording 99. ");
        FlightRecorderConnectionJava8 connection = new FlightRecorderConnectionJava8(mBeanServerConnection, objectName);
        long id = connection.startRecording(new RecordingOptions.Builder().build(), RecordingConfiguration.PROFILE_CONFIGURATION);
        Assert.assertEquals(id, 99);
    }
}
