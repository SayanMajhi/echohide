package io.echohide;

import io.echohide.config.ProjectConfig;
import java.io.IOException;

/** Phase 1 executable: checks the packaged configuration without claiming backend support. */
public final class EchoHide {
    private EchoHide() { }

    public static void main(String[] args) {
        if (args.length == 0 || (args.length == 1 && "--help".equals(args[0]))) {
            System.out.println("EchoHide 0.1 - Phase 1 foundation");
            System.out.println("Usage: java -jar target/echohide-0.1.0-SNAPSHOT.jar --check");
            System.out.println("Audio, encryption, steganography, TCP, and Swing arrive in later phases.");
            return;
        }
        if (args.length != 1 || !"--check".equals(args[0])) {
            System.err.println("Unsupported arguments. Use --help or --check.");
            System.exit(2);
        }
        try {
            ProjectConfig config = ProjectConfig.loadDefault();
            System.out.println("Phase 1 configuration check passed.");
            System.out.printf("Java: %s%n", System.getProperty("java.version"));
            System.out.printf("PBKDF2 iterations: %d%n", config.pbkdf2Iterations());
            System.out.printf("Default TCP port: %d%n", config.port());
            System.out.printf("Maximum file bytes: %d%n", config.maxFileBytes());
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }
}
