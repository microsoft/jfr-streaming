// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.jfr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration is used to configure a flight recording. To create a {@code Configuration},
 * use {@link Builder}.
 *
 * The configuration must be set at the time the {@code Recording} is created via
 * {@link FlightRecorderConnection#newRecording(RecordingOptions, RecordingConfiguration)}. A {@code Configuration}
 * is immutable which prevents attempts at changing the configuration while a recording
 * is in progress.
 */
public class RecordingOptions {

    /* Default values of some configuration elements. */
    private static final String EMPTY_STRING = "";
    private static final String NO_LIMIT = "0";

    /* Options hash table keys. */
    private enum Option {
        NAME("name", EMPTY_STRING),
        MAX_AGE("maxAge", NO_LIMIT),
        MAX_SIZE("maxSize", NO_LIMIT),
        DUMP_ON_EXIT("dumpOnExit", "false"),
        DESTINATION("destination", EMPTY_STRING),
        DISK("disk", "false"),
        DURATION("duration", NO_LIMIT);

        private Option(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }
        private final String name;
        private final String defaultValue; /* not null! */
    }

    /**
     * Builder for {@link RecordingOptions}.
     */
    public static class Builder {

        // The builder builds up a hash table of options. These options
        // correspond to the recording options for a FlightRecorderMXBean
        // Even though the input to the builder methods are not all String,
        // the option value is converted to a String. This simplifies
        // the code in the RecordingOption constructor, but makes the
        // RecordingOption getter methods a bit strange since they have to
        // covert from the String back to the raw value. The getter methods
        // could return String, but then you have the builder taking one type
        // and the getter returning a different type, which would be surprising.
        private final Map<Option, String> options = new HashMap<>();
        
        /**
         * Constructor for a {@code Builder}.
         */
        public Builder() {}

        /**
         * Sets the name for the recording file.
         * If the {@code name} is {@code null}, {@code name} will be set to the default value,
         * which an empty String. If {@code name} is the default value, the JVM will use the
         * recording id.
         * @param name The name for the recording.
         * @return {@code this}
         */
        public Builder name(String name) {
            options.put(
                    Option.NAME, 
                    name != null ? name.trim() : Option.NAME.defaultValue
            );
            return this;
        }

        /**
         * Sets the length of time that data is kept on disk before a recording <em>may</em>
         * be deleted. If the value is "0", no limit is imposed. Otherwise, the value is the
         * string representation of a positive long value followed by an empty space and one
         * of the following units:
         * <ul>
         *     <li>"ns" (nanoseconds)</li>
         *     <li>"us" (microseconds)</li>
         *     <li>"ms" (milliseconds)</li>
         *     <li>"s" (seconds)</li>
         *     <li>"m" (minutes)</li>
         *     <li>"h" (hours)</li>
         *     <li>"d" (days)</li>
         * </ul>
         * For example, {@code "2 h"}, {@code "3 m"}.
         *
         * If the value is {@code null}, {@code maxAge} will be set to the default value,
         * which is "no limit". The JVM will ignore this setting if {@link #disk(String) disk}
         * is set to false.
         *
         * @param maxAge The maximum length of time that data is kept on disk.
         * @return {@code this}
         * @throws IllegalArgumentException The {@code maxAge} parameter is not formatted correctly,
         * or represents a negative value
         */
        public Builder maxAge(String maxAge) throws IllegalArgumentException {
            options.put(
                    Option.MAX_AGE,
                    validateDuration(Option.MAX_AGE, maxAge)
            );
            return this;
        }

        /**
         * Sets the size, measured in bytes, at which data is kept on disk.
         * If the value is {@code null}, {@code maxSize} will be set to the default value,
         * which is "no limit". The JVM will ignore this setting if {@link #disk(String) disk}
         * is set to false.
         * @param maxSize The maximum size, in bytes.
         * @return {@code this}
         * @throws IllegalArgumentException The {@code maxSize} parameter is not a positive long value.
         */
        public Builder maxSize(String maxSize) throws IllegalArgumentException {
            long value = 0L;
            try {
                String numVal = maxSize == null || maxSize.trim().length() == 0 ? Option.MAX_SIZE.defaultValue : maxSize.trim();
                value = Long.parseLong(numVal);
                if (value < 0L) {
                    throw new IllegalArgumentException("maxSize: " + value + " < 0");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(e);
            }
            options.put(
                    Option.MAX_SIZE, 
                    Long.toString(value)
            );
            return this;
        }

        /**
         * If set to {@code "true"}, the JVM will dump recording data to disk on exit.
         * The default value is {@code "false"}. The {@code dumpOnExit} argument follows
         * the semantics of the {@code Boolean#valueOf(String)} method.
         * @param dumpOnExit Whether to dump recording data when the JVM exits.
         * @return {@code this}
         */
        public Builder dumpOnExit(String dumpOnExit) {
            options.put(
                    Option.DUMP_ON_EXIT, 
                    Boolean.valueOf(dumpOnExit).toString()
            );
            return this;
        }

        /**
         * Set the path where recording data is written when the recording stops.
         * The path is relevant to the machine where the JVM is running.
         * If the path is relative, it is relative to the working directory from which the JVM
         * was started.
         * If the {@code destination} is {@code null}, {@code destination} will be set to the
         * default value, which an empty String, and the JVM will use the directory from which
         * the JVM was started.
         * @param destination A path to where recording data will be written.
         * @return {@code this}
         */
        public Builder destination(String destination) {
            options.put(
                    Option.DESTINATION,
                    destination != null 
                            ? destination.trim()
                            : Option.DESTINATION.defaultValue
            );
            return this;
        }

        /**
         * If {@code "true"}, data will be stored to disk as it is recorded.
         * The default value is {@code "false"}. The {@code disk} argument follows
         * the semantics of the {@code Boolean#valueOf(String)} method.
         * @param disk Whether to store data as it is recorded.
         * @return {@code this}
         */
        public Builder disk(String disk) {
            options.put(
                    Option.DISK, 
                    Boolean.valueOf(disk).toString()
            );
            return this;
        }

        /**
         * Sets how long the recording should be running. The default is to have no limit.
         * If the value is "0", no limit is imposed. Otherwise, the value is the string
         * representation of a positive long value followed by an empty space and one
         * of the following units:
         * <ul>
         *     <li>"ns" (nanoseconds)</li>
         *     <li>"us" (microseconds)</li>
         *     <li>"ms" (milliseconds)</li>
         *     <li>"s" (seconds)</li>
         *     <li>"m" (minutes)</li>
         *     <li>"h" (hours)</li>
         *     <li>"d" (days)</li>
         * </ul>
         * For example, {@code "2 h"}, {@code "3 m"}.
         *
         * If the value is {@code null}, {@code duration} will be set to the default value,
         * which is "no limit".
         *
         * @param duration The maximum length of time a recording should be running.
         * @return {@code this}
         * @throws IllegalArgumentException The {@code duration} parameter is not formatted correctly,
         * or represents a negative value
         */
        public Builder duration(String duration) throws IllegalArgumentException {
            options.put(
                    Option.DURATION,
                    validateDuration(Option.DURATION, duration)
            );
            return this;
        }

        /**
         * Construct a {@code Configuration} from the options that were set on this builder.
         * The {@code Builder} state is not reset by calling this method.
         * @return A {@code Configuration}, never {@code null}.
         */
        public RecordingOptions build() {
            return new RecordingOptions(this);
        }

    }

    /**
     * Constructor is private. Only the Builder can construct a Configuration.
     * @param builder The builder that was used to parameterize the configuration
     */
    private RecordingOptions(Builder builder) {
        // Note we're converting from Map<Option<?>,String> to Map<String,String>
        final Map<Option,String> options = builder.options;
        //
        // For each option,
        //    if the option is not the default value
        //    add the option name and value to recordingOptions
        // An option is the default value if
        //     it was not set in the builder,
        //     it was not set as a system property,
        //     or was set in the builder, but to a default value (builder.setDuration(null), for example)
        // This stream does not modify builder.options.
        Map<String,String> initRecordingOptions =
                Stream.of(Option.values())
                        .filter(option -> !option.defaultValue.equals(options.getOrDefault(option, option.defaultValue)))
                        .collect(Collectors.toMap(option -> option.name, options::get));

        // Due to a bug, some JVMs default "disk=true". So include "disk=false" (the documented default)
        // to insure consistent behaviour.
        if (!initRecordingOptions.containsKey(Option.DISK.name)) {
            initRecordingOptions.put(Option.DISK.name, Option.DISK.defaultValue);
        }
        this.recordingOptions = Collections.unmodifiableMap(initRecordingOptions);
    }

    /**
     * Get the recording name. The return value will be an empty String if
     * the option has not been set.
     * @return The {@code name} recording option, or an empty String.
     */
    public String getName() {
        return recordingOptions.getOrDefault(Option.NAME.name, Option.NAME.defaultValue);
    }

    /**
     * Get the maximum length of time that data is kept on disk before it <em>may</em>
     * be deleted. The return value will be {@code "0""} if the option has not been set.
     * @return The {@code maxAge} recording option.
     */
    public String getMaxAge() {
        return recordingOptions.getOrDefault(Option.MAX_AGE.name, Option.MAX_AGE.defaultValue);
    }

    /**
     * Get the maximum size, in bytes, at which data is kept on disk. The return
     * value will be {@code "0"} if the option has not been set.
     * @return The {@code maxSize} recording option.
     */
    public String getMaxSize() {
        return recordingOptions.getOrDefault(Option.MAX_SIZE.name, Option.MAX_SIZE.defaultValue);
    }

    /**
     * Get whether to dump recording data to disk when the JVM exits.
     * @return {@code "true"} if recording data is dumped to disk on JVM exit.
     */
    public String getDumpOnExit() {
        return recordingOptions.getOrDefault(Option.DUMP_ON_EXIT.name, Option.DUMP_ON_EXIT.defaultValue);
    }

    /**
     * Get the path to where recording data is written. The path is relevant to the
     * machine on which the JVM is running. The return value will be an empty String
     * if the option has not been set.
     * @return The path to where recording data is written.
     */
    public String getDestination() {
        return recordingOptions.getOrDefault(Option.DESTINATION.name, Option.DESTINATION.defaultValue);
    }

    /**
     * Get whether to save flight recordings to disk.
     * @return {@code "true"} if flight recordings are saved to disk.
     */
    public String getDisk() {
        return recordingOptions.getOrDefault(Option.DISK.name, Option.DISK.defaultValue);
    }

    /**
     * Get how long the recording should be running. The return value will be
     * {@code "0"} if the option has not been set.
     * @return The {@code duration} for a recording.
     */
    public String getDuration() {
        return recordingOptions.getOrDefault(Option.DURATION.name, Option.DURATION.defaultValue);
    }

    /**
     * Get the read-only recording options. The keys are names of recording options
     * according to FlightRecorderMXBean.
     * @return A read-only map of the recording options.
     */
    /*package*/ Map<String,String> getRecordingOptions() {
        return recordingOptions;
    }

    // The configuration options. The keys are names of recording options
    // according to FlightRecorderMXBean.  The value is a valid value for
    // the option. Options that take on default values should be absent.
    private final Map<String,String> recordingOptions;

    // format for FlightRecorderMXBean maxAge and duration recording options
    private static final Pattern pattern = Pattern.compile("([-+]?\\d+)\\s*(\\w*)");

    // If the durationString arg is a valid format, return the arg properly formatted for FlightRecorderMXBean.
    // The expected format is a positive number, followed by a space (optional),
    // followed by the units (one of ns, us, ms, s, m, h, d).
    private static String validateDuration(Option option, String durationString) throws IllegalArgumentException {

        if (durationString == null || durationString.trim().length() == 0) {
            return option.defaultValue;
        }

        Matcher matcher = pattern.matcher(durationString);
        if (matcher.matches()) {

            final long value;
            try {
                value = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(e);
            }

            if (value >= 0L) {

                final String units = matcher.group(2);
                switch (units) {
                    case "":
                        return Long.toString(value);
                    case "ns":
                    case "us":
                    case "ms":
                    case "s":
                    case "m":
                    case "h":
                    case "d":
                        return Long.toString(value) + " " + units;
                }
            }
        }
        // TODO: i18n
        throw new IllegalArgumentException("bad format: " + option.name + " = \""+durationString+"\"");
    }

}
