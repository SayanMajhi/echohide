package io.echohide.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/** Public application settings. These values never contain message secrets. */
public record ProjectConfig(int pbkdf2Iterations, int port,
                            int connectTimeoutMs, int readTimeoutMs, long maxFileBytes) {
    public static final int MIN_PBKDF2_ITERATIONS = 600_000;
    public static final int MAX_PBKDF2_ITERATIONS = 2_000_000;
    public static final int MAX_TIMEOUT_MS = 300_000;
    public static final long MAX_FILE_BYTES = 100L * 1024 * 1024;

    public ProjectConfig {
        requireRange("PBKDF2 iterations", pbkdf2Iterations,
                MIN_PBKDF2_ITERATIONS, MAX_PBKDF2_ITERATIONS);
        requireRange("port", port, 1, 65_535);
        requireRange("connect timeout", connectTimeoutMs, 1, MAX_TIMEOUT_MS);
        requireRange("read timeout", readTimeoutMs, 1, MAX_TIMEOUT_MS);
        if (maxFileBytes < 1 || maxFileBytes > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File limit must be 1 to " + MAX_FILE_BYTES + " bytes.");
        }
    }

    public static ProjectConfig loadDefault() throws IOException {
        try (InputStream input = ProjectConfig.class.getResourceAsStream("/echohide.properties")) {
            if (input == null) {
                throw new IOException("Missing bundled echohide.properties.");
            }
            return load(input);
        }
    }

    /** Reads but does not close the caller's stream. */
    public static ProjectConfig load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        Properties properties = new Properties();
        properties.load(input);
        try {
            return new ProjectConfig(
                    Integer.parseInt(required(properties, "crypto.pbkdf2.iterations")),
                    Integer.parseInt(required(properties, "network.port")),
                    Integer.parseInt(required(properties, "network.connect.timeout.ms")),
                    Integer.parseInt(required(properties, "network.read.timeout.ms")),
                    Long.parseLong(required(properties, "network.max.file.bytes")));
        } catch (IllegalArgumentException exception) {
            // Do not echo arbitrary configuration content into diagnostics.
            throw new IOException("Invalid EchoHide configuration: check required settings and ranges.", exception);
        }
    }

    private static String required(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required setting: " + name);
        }
        return value.trim();
    }

    private static void requireRange(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be " + minimum + " to " + maximum + '.');
        }
    }
}
