package com.microsoft.jfr;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A flight recorder configuration controls the amount of data that is collected.
 */
public abstract class RecordingConfiguration<C> {

    /**
     * Convenience for selecting the pre-defined 'default' configuration that is standard with the JDK.
     * The default configuration is suitable for continuous recordings.
     */
    public static final RecordingConfiguration DEFAULT_CONFIGURATION = new PredefinedConfiguration("default");

    /**
     * Convenience for referencing the 'profile' configuration that is standard with the JDK.
     * The profile configuration collects more events and is suitable for profiling an application.
     */
    public static final RecordingConfiguration PROFILE_CONFIGURATION = new PredefinedConfiguration("profile");



    /**
     * A pre-defined configuration is one which you could select with the 'settings' option
     * of the JVM option 'StartFlightRecording', for example {@code -XX:StartFlightRecording:settings=default.jfc}.
     */
    public static class PredefinedConfiguration extends RecordingConfiguration<String> {
        @Override
        public String getMbeanSetterFunction() {
            return "setPredefinedConfiguration";
        }

        /**
         * Sets a pre-defined configuration to use with a {@code Recording}.
         *
         * @param configurationName The name of the pre-defined configuration, not {@code null}.
         * @throws NullPointerException if predefinedConfiguration is {@code null}
         */
        public PredefinedConfiguration(String configurationName) {
            super(configurationName);
        }

    }

    /**
     * A configuration that is read from a jfc file
     */
    public static class JfcFileConfiguration extends RecordingConfiguration<String> {

        /**
         * Sets a configuration from a jfc file to use with a {@code Recording}.
         *
         * @param configurationFile An InputStream containing the configuration file, not {@code null}.
         * @throws NullPointerException if predefinedConfiguration is {@code null}
         */
        public JfcFileConfiguration(InputStream configurationFile) {
            super(readConfigurationFile(configurationFile));
        }

        private static String readConfigurationFile(InputStream inputStream) {
            if (inputStream != null) {
                return new BufferedReader(new InputStreamReader(inputStream))
                        .lines()
                        .collect(Collectors.joining());
            } else {
                throw new IllegalArgumentException("Null configuration provided");
            }
        }

        @Override
        public String getMbeanSetterFunction() {
            return "setConfiguration";
        }
    }

    /**
     * A confiration defined from a map.
     */
    public static class MapConfiguration extends RecordingConfiguration<Map<String, String>> {

        /**
         * Sets a configuration from a Map
         * @param configuration A map defining the JFR events to register.
         *                      For example: {jdk.ObjectAllocationInNewTLAB#enabled=true, jdk.ObjectAllocationOutsideTLAB#enabled=true}
         */
        public MapConfiguration(Map<String, String> configuration) {
            super(configuration);
        }

        @Override
        public String getMbeanSetterFunction() {
            return "setRecordingSettings";
        }

    }

    /**
     * Get the setter function on the FlightRecorder mbean that receives this configuration type
     * @return The setter function on the FlightRecorder mbean that receives this configuration type
     */
    public abstract String getMbeanSetterFunction();

    /**
     * Sets a configuration to use with a {@code Recording}.
     * @param configuration The value of the configuration, not {@code null}.
     * @throws NullPointerException if configuration is {@code null}
     */
    public RecordingConfiguration(C configuration) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        this.configuration = configuration;
    }

    /**
     * Get the recording configuration.
     * @return The recording configuration.
     */
    public C getConfiguration() {
        return configuration;
    }

    /** The configuration name or file contents. */
    protected final C configuration;

}
