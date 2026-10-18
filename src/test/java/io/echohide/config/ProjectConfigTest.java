package io.echohide.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ProjectConfigTest {
    @Test
    void bundledConfigurationIsValid() throws IOException {
        ProjectConfig config = ProjectConfig.loadDefault();
        assertEquals(600_000, config.pbkdf2Iterations());
        assertEquals(5050, config.port());
        assertEquals(5000, config.connectTimeoutMs());
        assertEquals(15_000, config.readTimeoutMs());
        assertEquals(104_857_600L, config.maxFileBytes());
    }

    @ParameterizedTest
    @CsvSource({
        "network.port,0", "network.port,65536", "network.port,invalid",
        "crypto.pbkdf2.iterations,599999", "crypto.pbkdf2.iterations,2000001",
        "network.connect.timeout.ms,0", "network.connect.timeout.ms,300001",
        "network.read.timeout.ms,-1", "network.read.timeout.ms,300001",
        "network.max.file.bytes,0", "network.max.file.bytes,104857601",
        "network.max.file.bytes,9223372036854775808"
    })
    void rejectsUnsafeOrMalformedSettings(String key, String value) throws IOException {
        Properties properties = defaults();
        properties.setProperty(key, value);
        assertThrows(IOException.class, () -> ProjectConfig.load(stream(properties)));
    }

    @Test
    void rejectsMissingSettings() {
        assertThrows(IOException.class, () -> ProjectConfig.load(
                new ByteArrayInputStream("network.port=5050".getBytes(StandardCharsets.ISO_8859_1))));
    }

    @Test
    void rejectsBlankSettings() throws IOException {
        Properties properties = defaults();
        properties.setProperty("network.port", "   ");
        assertThrows(IOException.class, () -> ProjectConfig.load(stream(properties)));
    }

    @Test
    void acceptsDocumentedLimits() {
        assertDoesNotThrow(() -> new ProjectConfig(600_000, 1, 1, 1, 1));
        assertDoesNotThrow(() -> new ProjectConfig(2_000_000, 65535, 300_000, 300_000, 104_857_600));
    }

    private static Properties defaults() throws IOException {
        Properties properties = new Properties();
        try (var input = ProjectConfigTest.class.getResourceAsStream("/echohide.properties")) {
            assertNotNull(input);
            properties.load(input);
        }
        return properties;
    }

    private static ByteArrayInputStream stream(Properties properties) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        properties.store(output, null);
        return new ByteArrayInputStream(output.toByteArray());
    }
}
