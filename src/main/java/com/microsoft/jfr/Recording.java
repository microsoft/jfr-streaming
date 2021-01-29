// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.jfr;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a means to start, stop and dump recording data.
 * To create a {@code Recording}, use one of the {@code newRecording} methods
 * in {@link FlightRecorderConnection}.
 * @see FlightRecorderConnection#newRecording(RecordingOptions, RecordingConfiguration)
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/jdk.jfr/jdk/jfr/Recording.html">jdk.jfr.Recording</a>
 */
public class Recording implements AutoCloseable {

    /**
     * A {@code Recording} may be in one of these states. Note that a {@code Recording} is
     * no longer usable once it is in the {@code CLOSED} state.
     */
    public enum State {
        /**
         * The {@code Recording} has been created.
         */
        NEW,
        /**
         * The {@code Recording} has been started.
         */
        RECORDING,
        /**
         * The {@code Recording} has been stopped.
         */
        STOPPED,
        /**
         * The {@code Recording} has been closed. Once the
         * recording has been closed, it is no longer usable.
         */
        CLOSED;
    }

    // Format for IllegalStateException that this class might throw
    private final static MessageFormat illegalStateFormat = new MessageFormat("Recording state {0} not in [{1}]");
    private static String createIllegalStateExceptionMessage(State actual, State expected, State... others) {
        String[] args = new String[]{actual.name(), expected.name()};
        if (others != null) {
            for (State state : others) args[1] = args[1].concat(", ").concat(state.name());
        }
        String  msg = illegalStateFormat.format(args);
        return msg;
    }

    final private FlightRecorderConnection connection;
    final private RecordingOptions recordingOptions;
    final private RecordingConfiguration recordingConfiguration;

    private volatile long id = -1;
    private final AtomicReference<State> state;

    /**
     * Create a {@code Recording}. Recordings are created from
     * {@link FlightRecorderConnection#newRecording(RecordingOptions, RecordingConfiguration)}
     * @param connection A connection to the FlightRecorderMXBean on an MBean server
     * @param recordingOptions The options to be used for the recording
     * @param recordingConfiguration The settings for events to be collected by the recording
     */
    /*package*/ Recording(
            FlightRecorderConnection connection,
            RecordingOptions recordingOptions,
            RecordingConfiguration recordingConfiguration) {
        this.connection = connection;
        this.recordingOptions = recordingOptions;
        this.recordingConfiguration = recordingConfiguration;
        this.state = new AtomicReference<>(State.NEW);
    }

    /**
     * Get the recording id. The recording does not have an id until the recording is started.
     * @return The recording id, or {@code -1} if the recording was never started.
     */
    public long getId() throws IllegalStateException {
        return id;
    }

    /**
     * Start a recording. A recording may not be started after it is closed.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws IllegalStateException This {@code Recording} is closed.
     * @throws InternalError Wraps a {@code javax.management.JMException}. See {@link InternalError}.
     * @return The recording id. 
     */
    public long start() throws IOException, IllegalStateException {
        // state machine transitions: NEW -> RECORDING or STOPPED -> RECORDING, otherwise remain in state
        State oldState = state.getAndUpdate(s -> s == State.NEW || s == State.STOPPED ? State.RECORDING : s);

        if (oldState == State.NEW || oldState == State.STOPPED) {
            id = connection.startRecording(recordingOptions, recordingConfiguration);
        } else if (oldState == State.CLOSED) {
            throw new IllegalStateException(createIllegalStateExceptionMessage(oldState, State.NEW, State.RECORDING, State.STOPPED));
        }
        return id;
    }

    /**
     * Stop a recording.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws IllegalStateException If the {@code Recording} is closed.
     * @throws InternalError Wraps a {@code javax.management.JMException}. See {@link InternalError}.
     */
    public void stop() throws IOException, IllegalStateException {
        // state machine transitions:  RECORDING -> STOPPED, otherwise remain in state
        State oldState = state.getAndUpdate(s -> s == State.RECORDING ? State.STOPPED : s);
        if (oldState == State.RECORDING) {
            connection.stopRecording(id);
        } else if (oldState == State.CLOSED) {
            throw new IllegalStateException(createIllegalStateExceptionMessage(oldState, State.NEW, State.RECORDING, State.STOPPED));
        }
    }

    /**
     * Writes recording data to the specified file. The recording must be started, but not necessarily stopped.
     * The {@code outputFile} argument is relevant to the machine where the JVM is running.
     * @param outputFile the system-dependent file name where data is written, not {@code null}
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws IllegalStateException If the {@code Recording} has not been started, or has been closed.
     * @throws InternalError Wraps a {@code javax.management.JMException}. See {@link InternalError}.
     * @throws NullPointerException If the {@code outputFile} argument is null.
     */
    public void dump(String outputFile) throws IOException, IllegalStateException {
        Objects.requireNonNull(outputFile, "outputFile may not be null");
        State currentState = state.get();
        if (currentState == State.RECORDING || currentState == State.STOPPED) {
            connection.dumpRecording(id, outputFile);
        } else {
            throw new IllegalStateException(createIllegalStateExceptionMessage(currentState, State.RECORDING, State.STOPPED));
        }
    }

    /**
     * Creates a copy of an existing recording, useful for extracting parts of a recording.
     * The cloned recording contains the same recording data as the original, but it has a
     * new ID. If the original recording is running, then the clone is also running.
     * @param stop Whether to stop the cloned recording.
     * @return The cloned recording.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws IllegalStateException If the {@code Recording} has not been started, or has been closed.
     * @throws InternalError Wraps a {@code javax.management.JMException}. See {@link InternalError}.
     */
    public Recording clone(boolean stop) throws IOException {
        State currentState = state.get();
        if (currentState == State.RECORDING || currentState == State.STOPPED) {
            long newId = connection.cloneRecording(id, stop);
            Recording recordingClone = new Recording(this.connection, this.recordingOptions, this.recordingConfiguration);
            recordingClone.id = newId;
            recordingClone.state.set(stop ? State.STOPPED : currentState);
            return recordingClone;
        } else {
            throw new IllegalStateException(createIllegalStateExceptionMessage(currentState, State.RECORDING, State.STOPPED));
        }
    }

    /**
     * Create a data stream for the specified interval using the default {@code blockSize}.
     * The stream may contain some data outside the given range.
     * @param startTime The start time for the stream, or {@code null} to get data from the start time of the recording.
     * @param endTime The end time for the stream, or {@code null} to get data until the end of the recording.
     * @return An {@code InputStream}, or {@code null} if no data is available in the interval.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws IllegalStateException If the {@code Recording} has not been stopped.
     * @throws InternalError Wraps a {@code javax.management.JMException}. See {@link InternalError}.
     * @see JfrStream#getDefaultBlockSize()
     */
    public InputStream getStream(Instant startTime, Instant endTime) throws IOException, IllegalStateException {
        return getStream(startTime, endTime, JfrStream.getDefaultBlockSize());
    }

    /**
     * Create a data stream for the specified interval using the given {@code blockSize}.
     * The stream may contain some data outside the given range.
     * The {@code blockSize} is used to configure the maximum number of bytes to read
     * from underlying stream at a time. Setting blockSize to a very high value may result
     * in an exception, if the Java Virtual Machine (JVM) deems the value too large to handle.
     * Refer to the javadoc for {@code FlightRecorderMXBean#openStream}.
     * @param startTime The start time for the stream, or {@code null} to get data from the start time of the recording.
     * @param endTime The end time for the stream, or {@code null} to get data until the end of the recording.
     * @param blockSize The maximum number of bytes to read at a time.
     * @return An {@code InputStream}, or {@code null} if no data is available in the interval.
     * @throws IOException A communication problem occurred when talking to the MBean server.
     * @throws IllegalStateException If the {@code Recording} has not been stopped.
     * @throws InternalError Wraps a {@code javax.management.JMException}. See {@link InternalError}.
     */
    public InputStream getStream(Instant startTime, Instant endTime, long blockSize)
            throws IOException, IllegalStateException {
        // state machine transitions: remain in state
        State currentState = state.get();
        if (currentState == State.STOPPED) {
            return connection.getStream(id, startTime, endTime, blockSize);
        } else {
            throw new IllegalStateException(createIllegalStateExceptionMessage(currentState, State.STOPPED));
        }
    }

    /**
     * Get the current state of this {@code Recording}.
     * @return The current state of this {@code Recording}.
     */
    public State getState() {
        return state.get();
    }

    @Override
    public void close() throws IOException {
        // state machine transitions:  any -> CLOSED
        State oldState = state.getAndSet(State.CLOSED);
        if (oldState == State.RECORDING) {
            try {
                connection.stopRecording(id);
                connection.closeRecording(id);
            } catch (Throwable ignored) {
            }
        }
    }
}
