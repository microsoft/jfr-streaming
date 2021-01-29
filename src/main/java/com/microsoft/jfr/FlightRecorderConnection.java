// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.jfr;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

/**
 * Represents a connection to a {@code jdk.management.jfr.FlightRecorderMXBean} of a JVM.
 * {@code FlightRecorderConnection} provides {@link #newRecording(RecordingOptions, RecordingConfiguration) API} to create
 * Java flight {@link Recording recordings}. More than one {@code Recording} can be created.
 *
 * To use this class, a {@code javax.management.MBeanServerConnection} is needed.
 * This class uses the connection to make calls to the MBean server and does not change
 * the state of the connection. Management of the connection is the concern of the caller
 * and use of a {@code FlightRecorderConnection} for an MBean server connection that is no
 * longer valid will result in {@code IOException} being thrown.
 *
 * The {@code MBeanServerConnection} can be a connection to any MBean server.
 * Typically, the connection is to the platform MBean server obtained by calling
 * {@code java.lang.management.ManagementFactory.getPlatformMBeanServer()}. The connection can
 * also be to a remote MBean server via {@code javax.management.remote.JMXConnector}.
 * Refer to the summary in the javadoc of the {@code javax.management} package and of the
 * {@code javax.management.remote} package for details.
 *
 */
public class FlightRecorderConnection {

    private static final String JFR_OBJECT_NAME = "jdk.management.jfr:type=FlightRecorder";

    /**
     * Create a connection to the {@code FlightRecorder} via JMX. This method either returns a
     * {@code FlightRecorderConnection}, or throws an exception. An {@code IOException}
     * indicates a problem with the connection to the MBean server. An {@code InstanceNotFoundException}
     * indicates that the FlightRecorder MBean is not registered on the target JVM. This could happen
     * if the target JVM does not support Java Flight Recorder, or if expermental features need to be
     * enabled on the target JVM.
     *
     * @param mBeanServerConnection The {@code MBeanServerConnection} to the JVM.
     * @return A {@code FlightRecorderConnection}.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws InstanceNotFoundException The FlightRecorder MBean is not registered on the target JVM.
     * @throws java.lang.InternalError Wraps a {@code javax.management.MalformedObjectNameException}
     * and indicates a bug in this class.
     * @throws NullPointerException The {@code mBeanServerConnection} parameter is {@code null}.
     */
    public static FlightRecorderConnection connect(MBeanServerConnection mBeanServerConnection)
            throws IOException, InstanceNotFoundException
    {
        Objects.requireNonNull(mBeanServerConnection);
        try {
            ObjectName objectName = new ObjectName(JFR_OBJECT_NAME);
            ObjectInstance objectInstance = mBeanServerConnection.getObjectInstance(objectName);
            return new FlightRecorderConnection(mBeanServerConnection, objectInstance.getObjectName());
        } catch (MalformedObjectNameException e) {
            // Not expected to happen. This exception comes from the ObjectName constructor. If
            // JFR_OBJECT_NAME is malformed, then this is an internal bug.
            throw new InternalError(JFR_OBJECT_NAME, e);
        }
    }

    /**
     * Create a {@link Recording} with the given configuration. The {@code Recording} is created
     * in the {@link Recording.State#NEW} state. If {@code null} is passed for the
     * {@code configuration} parameter, the recording will use the default values of
     * {@code jdk.management.jfr.FlightRecorderMXBean}.
     * @param recordingOptions The configuration to be used for the recording.
     * @return A {@link Recording} object associated with this {@code FlightRecorderConnection}.
     */
    public Recording newRecording(
            RecordingOptions recordingOptions,
            RecordingConfiguration recordingConfiguration) {
        return new Recording(this, recordingOptions, recordingConfiguration);
    }

    /**
     * Start a recording. This method creates a new recording, sets the configuration, and then starts the recording.
     * This method is called from the {@link Recording#start()} method.
     * @param recordingOptions The {@code RecordingOptions} which was passed to
     *                         the {@link #newRecording(RecordingOptions, RecordingConfiguration)} method
     * @param recordingConfiguration The {@code RecordingConfiguration} which was passed to
     *                          the {@link #newRecording(RecordingOptions, RecordingConfiguration)} method
     * @return The id of the recording.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws java.lang.InternalError Wraps an {@code javax.management.InstanceNotFoundException},
     * a {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
     * and indicates an issue with the FlightRecorderMXBean in the JVM.
     * The cause may also be a {@code javax.management.openmbean.OpenDataException}
     * which indicates a bug in the code of this class.
     */
    long startRecording(RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration)
            throws IOException {

        try {
            final long id = (long) connection.invoke(
                    flightRecorder,
                    "newRecording",
                    new Object[]{},
                    new String[]{}
            );

            if (recordingConfiguration != null) {
                String predefinedConfiguration = recordingConfiguration.getName();
                if (predefinedConfiguration != null && predefinedConfiguration.trim().length() > 0) {
                    connection.invoke(
                            flightRecorder,
                            "setPredefinedConfiguration",
                            new Object[]{id, predefinedConfiguration},
                            new String[]{long.class.getName(), String.class.getName()}
                    );
                }
            }

            if (recordingOptions != null) {
                Map<String,String> options = recordingOptions.getRecordingOptions();
                if (options != null && !options.isEmpty()) {
                    TabularData recordingOptionsParam = makeOpenData(options);
                    connection.invoke(
                            flightRecorder,
                            "setRecordingOptions",
                            new Object[]{id, recordingOptionsParam},
                            new String[]{long.class.getName(), TabularData.class.getName()}
                    );
                }
            }

            connection.invoke(
                    flightRecorder,
                    "startRecording",
                    new Object[]{id},
                    new String[]{long.class.getName()}
            );

            return id;
        } catch (OpenDataException|InstanceNotFoundException|MBeanException|ReflectionException e) {
            // In theory, we should never get these.
            throw new InternalError(e.getMessage(), e);
        }
    }

    /**
     * Stop a recording. This method is called from the {@link Recording#stop()} method.
     * @param id The id of the recording.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws java.lang.InternalError Wraps an {@code javax.management.InstanceNotFoundException},
     * a {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
     * and indicates an issue with the FlightRecorderMXBean in the JVM.
     */
    void stopRecording(long id) throws IOException {
        try {
            connection.invoke(flightRecorder, "stopRecording", new Object[]{id}, new String[]{long.class.getName()});
        } catch (InstanceNotFoundException|MBeanException|ReflectionException e) {
            throw new InternalError(e.getMessage(), e);
        }
    }

    /**
     * Writes recording data to the specified file. The recording must be started, but not necessarily stopped.
     * The {@code outputFile} argument is relevant to the machine where the JVM is running.
     * @param id The id of the recording.
     * @param outputFile the system-dependent file name where data is written, not {@code null}
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws InternalError Wraps a {@code javax.management.JMException}. See {@link InternalError}.
     */
    void dumpRecording(long id, String outputFile) throws IOException {
        try {
            connection.invoke(flightRecorder, "copyTo", new Object[]{id,outputFile}, new String[]{long.class.getName(),String.class.getName()});
        } catch (InstanceNotFoundException|MBeanException|ReflectionException e) {
            throw new InternalError(e.getMessage(), e);
        }
    }

    /**
     * Creates a copy of an existing recording, useful for extracting parts of a recording.
     * The cloned recording contains the same recording data as the original, but it has a
     * new ID. If the original recording is running, then the clone is also running.
     * @param id The id of the recording being cloned.
     * @param stop Whether to stop the cloned recording.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws InternalError Wraps a {@code javax.management.JMException}. See {@link InternalError}.
     */
    long cloneRecording(long id, boolean stop) throws IOException {
        try {
            return (long)connection.invoke(flightRecorder, "cloneRecording", new Object[]{id,stop}, new String[]{long.class.getName(),boolean.class.getName()});
        } catch (InstanceNotFoundException|MBeanException|ReflectionException e) {
            throw new InternalError(e.getMessage(), e);
        }
    }

    /**
     * Get the Java Flight Recording as an {@code java.io.InputStream.
     * This method is called from the {@link Recording#getStream(Instant, Instant, long)} method.
     *
     * The recording may contain data outside the {@code startTime} and {@code endTime} parameters.
     * Either or both of {@code startTime} and {@code endTime} may be {@code null}, in which case the
     * {@code FlightRecorderMXBean} will use a default value indicating the beginning and the end of the
     * recording, respectively.
     *
     * The {@code blockSize} parameter specifies the number of bytes to read with a call to
     * the {@code FlightRecorderMXBean#readStream(long)} method. Setting blockSize to a very high value
     * may result in an OutOfMemoryError or an IllegalArgumentException, if the JVM deems the value too
     * large to handle.
     *
     * @param id The id of the recording.
     * @param startTime The point in time to start the recording stream, possibly {@code null}.
     * @param endTime The point in time to end the recording stream, possibly {@code null}.
     * @param blockSize The number of bytes to read at a time.
     * @return A {@code InputStream} of the Java Flight Recording data.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws InternalError Wraps an {@code javax.management.InstanceNotFoundException},
     * a {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
     * and indicates an issue with the FlightRecorderMXBean in the JVM.
     * The cause may also be a {@code javax.management.openmbean.OpenDataException}
     * which indicates a bug in the code of this class.
     */
    InputStream getStream(long id, Instant startTime, Instant endTime, long blockSize)
            throws IOException {
        Map<String,String> options = new HashMap<>();
        if (startTime != null) options.put("startTime", startTime.toString());
        if (endTime != null)   options.put("endTime",   endTime.toString());
        if (blockSize > 0)     options.put("blockSize", Long.toString(blockSize));

        try {
            TabularData streamOptions = makeOpenData(options);
            long streamId = (long) connection.invoke(flightRecorder, "openStream", new Object[]{id, streamOptions}, new String[]{long.class.getName(), TabularData.class.getName()});
            return new JfrStream(connection, flightRecorder, streamId);
        } catch(OpenDataException|InstanceNotFoundException|MBeanException|ReflectionException e) {
            throw new InternalError(e.getMessage(), e);
        }
    }

    /**
     * Close the recording. This method is called from the {@link Recording#close()} method.
     * @param id The id of the recording.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws java.lang.InternalError Wraps an {@code javax.management.InstanceNotFoundException},
     * a {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
     * and indicates an issue with the FlightRecorderMXBean in the JVM.
     */
    void closeRecording(long id) throws IOException {
        try {
            connection.invoke(flightRecorder, "closeRecording", new Object[]{id}, new String[]{long.class.getName()});
        } catch (InstanceNotFoundException|MBeanException|ReflectionException e) {
           throw new InternalError(e.getMessage(), e);
        }
    }

    private FlightRecorderConnection(MBeanServerConnection connection, ObjectName objectName) {
        this.connection = connection;
        this.flightRecorder = objectName;
    }

    private final MBeanServerConnection connection;
    private final ObjectName flightRecorder;

    /**
     * Convert the Map to TabularData
     * @param options A map of key-value pairs.
     * @return TabularData
     * @throws OpenDataException Can only be raised if there is a bug in this code.
     */
    private static TabularData makeOpenData(final Map<String, String> options) throws OpenDataException {
        // Copied from newrelic-jfr-core
        final String typeName = "java.util.Map<java.lang.String, java.lang.String>";
        final String[] itemNames = new String[]{"key", "value"};
        final OpenType<?>[] openTypes = new OpenType[]{SimpleType.STRING, SimpleType.STRING};
        final CompositeType rowType = new CompositeType(typeName, typeName, itemNames, itemNames, openTypes);
        final TabularType tabularType = new TabularType(typeName, typeName, rowType, new String[]{"key"});
        final TabularDataSupport table = new TabularDataSupport(tabularType);

        for (Map.Entry<String, String> entry : options.entrySet()) {
            Object[] itemValues = {entry.getKey(), entry.getValue()};
            CompositeData element = new CompositeDataSupport(rowType, itemNames, itemValues);
            table.put(element);
        }
        return table;
    }

}
