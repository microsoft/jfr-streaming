package com.microsoft.jfr;

import com.microsoft.jfr.flightRecorderConnections.FlightRecorderConnectionDefault;
import com.microsoft.jfr.flightRecorderConnections.FlightRecorderConnectionJava8;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;

public interface FlightRecorderConnection {
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
     * @throws IOException               A communication problem occurred when talking to the MBean server.
     * @throws InstanceNotFoundException The FlightRecorder MBean is not registered on the target JVM.
     * @throws JfrStreamingException     Wraps a {@code javax.management.MalformedObjectNameException}
     *                                   and indicates a bug in this class.
     * @throws NullPointerException      The {@code mBeanServerConnection} parameter is {@code null}.
     */
    static FlightRecorderConnection connect(MBeanServerConnection mBeanServerConnection)
            throws IOException, InstanceNotFoundException, JfrStreamingException {
        Objects.requireNonNull(mBeanServerConnection);
        try {
            return FlightRecorderConnectionDefault.connect(mBeanServerConnection);
        } catch (InstanceNotFoundException e) {
            //Try a Java 8 connection
            return FlightRecorderConnectionJava8.connect(mBeanServerConnection);
        }
    }

    /**
     * Create a {@link Recording} with the given options and configuration. The {@code Recording} is created
     * in the {@link Recording.State#NEW} state. The recording will use the default values of
     * {@code jdk.management.jfr.FlightRecorderMXBean} for a parameter passed as {@code null}.
     *
     * @param recordingOptions       The options to be used for the recording, or {@code null} for defaults.
     * @param recordingConfiguration The configuration to be used for the recording, or {@code null} for defaults.
     * @return A {@link Recording} object associated with this {@code FlightRecorderConnection}.
     */
    Recording newRecording(
            RecordingOptions recordingOptions,
            RecordingConfiguration recordingConfiguration);

    /**
     * Start a recording. This method creates a new recording, sets the configuration, and then starts the recording.
     * This method is called from the {@link Recording#start()} method.
     *
     * @param recordingOptions       The {@code RecordingOptions} which was passed to
     *                               the {@link #newRecording(RecordingOptions, RecordingConfiguration)} method
     * @param recordingConfiguration The {@code RecordingConfiguration} which was passed to
     *                               the {@link #newRecording(RecordingOptions, RecordingConfiguration)} method
     * @return The id of the recording.
     * @throws IOException           A communication problem occurred when talking to the MBean server.
     * @throws JfrStreamingException Wraps an {@code javax.management.InstanceNotFoundException},
     *                               a {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
     *                               and indicates an issue with the FlightRecorderMXBean in the JVM.
     *                               The cause may also be a {@code javax.management.openmbean.OpenDataException}
     *                               which indicates a bug in the code of this class.
     */
    long startRecording(RecordingOptions recordingOptions, RecordingConfiguration recordingConfiguration) throws IOException, JfrStreamingException;

    /**
     * Stop a recording. This method is called from the {@link Recording#stop()} method.
     *
     * @param id The id of the recording.
     * @throws IOException           A communication problem occurred when talking to the MBean server.
     * @throws JfrStreamingException Wraps an {@code javax.management.InstanceNotFoundException},
     *                               a {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
     *                               and indicates an issue with the FlightRecorderMXBean in the JVM.
     */
    void stopRecording(long id) throws IOException, JfrStreamingException;

    /**
     * Writes recording data to the specified file. The recording must be started, but not necessarily stopped.
     * The {@code outputFile} argument is relevant to the machine where the JVM is running.
     *
     * @param id         The id of the recording.
     * @param outputFile the system-dependent file name where data is written, not {@code null}
     * @throws IOException           A communication problem occurred when talking to the MBean server.
     * @throws JfrStreamingException Wraps a {@code javax.management.JMException}.
     */
    void dumpRecording(long id, String outputFile) throws IOException, JfrStreamingException;

    /**
     * Creates a copy of an existing recording, useful for extracting parts of a recording.
     * The cloned recording contains the same recording data as the original, but it has a
     * new ID. If the original recording is running, then the clone is also running.
     *
     * @param id   The id of the recording being cloned.
     * @param stop Whether to stop the cloned recording.
     * @throws IOException           A communication problem occurred when talking to the MBean server.
     * @throws JfrStreamingException Wraps a {@code javax.management.JMException}.
     * @return id of the recording
     */
    long cloneRecording(long id, boolean stop) throws IOException, JfrStreamingException;

    /**
     * Get the Java Flight Recording as an {@code java.io.InputStream}.
     * This method is called from the {@link Recording#getStream(Instant, Instant, long)} method.
     * <p>
     * The recording may contain data outside the {@code startTime} and {@code endTime} parameters.
     * Either or both of {@code startTime} and {@code endTime} may be {@code null}, in which case the
     * {@code FlightRecorderMXBean} will use a default value indicating the beginning and the end of the
     * recording, respectively.
     * <p>
     * The {@code blockSize} parameter specifies the number of bytes to read with a call to
     * the {@code FlightRecorderMXBean#readStream(long)} method. Setting blockSize to a very high value
     * may result in an OutOfMemoryError or an IllegalArgumentException, if the JVM deems the value too
     * large to handle.
     *
     * @param id        The id of the recording.
     * @param startTime The point in time to start the recording stream, possibly {@code null}.
     * @param endTime   The point in time to end the recording stream, possibly {@code null}.
     * @param blockSize The number of bytes to relonead at a time.
     * @return A {@code InputStream} of the Java Flight Recording data.
     * @throws IOException           A communication problem occurred when talking to the MBean server.
     * @throws JfrStreamingException Wraps an {@code javax.management.InstanceNotFoundException},
     *                               a {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
     *                               and indicates an issue with the FlightRecorderMXBean in the JVM.
     *                               The cause may also be a {@code javax.management.openmbean.OpenDataException}
     *                               which indicates a bug in the code of this class.
     */
    InputStream getStream(long id, Instant startTime, Instant endTime, long blockSize) throws IOException, JfrStreamingException;


    /**
     * Close the recording. This method is called from the {@link Recording#close()} method.
     *
     * @param id The id of the recording.
     * @throws IOException           A communication problem occurred when talking to the MBean server.
     * @throws JfrStreamingException Wraps an {@code javax.management.InstanceNotFoundException},
     *                               a {@code javax.management.MBeanException} or a {@code javax.management.ReflectionException}
     *                               and indicates an issue with the FlightRecorderMXBean in the JVM.
     */
    void closeRecording(long id) throws IOException, JfrStreamingException;
}
