# Phase 1 verification record

Executed on 16 October 2026 using the user's Asia/Calcutta date context. The first Maven build completed at `2026-10-16T01:19:02+05:30`. Runtime: Oracle Java 21.0.11; Maven 3.9.9 downloaded locally with a verified SHA-512 archive.

| Executed check | Observed result |
|---|---|
| `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\maven.ps1 clean verify` | Exit 0, `BUILD SUCCESS`; first build reported 7.673 s (excludes launcher download) |
| JUnit Surefire report | 16 tests, 0 failures, 0 errors, 0 skipped; test class elapsed 0.206 s |
| Packaged JAR `--check` | Exit 0, configuration check passed; Java 21.0.11, 600000 iterations, port 5050, 104857600-byte cap |
| Packaged JAR `--help` | Exit 0, foundation scope and usage printed |
| Packaged JAR `--unknown` | Exit 2, clear unsupported-argument error |
| `git init -b main` | Local repository initialized; no commit or remote created |
| `git check-ignore` for Maven binary and generated JAR | Both `.tools` and `target` correctly ignored |

JUnit evidence: `target/surefire-reports/io.echohide.config.ProjectConfigTest.txt` and its corresponding `TEST-io.echohide.config.ProjectConfigTest.xml`. These are local generated reports and regenerate on the next build.

JAR: `target/echohide-0.1.0-SNAPSHOT.jar`. SHA-256 of this build: `482BC91930225CC6CEAD0367467543347E58A9DF342E57201CA7AA7E5F4C0FF7`. Build timestamps can change the JAR hash on rebuild; this records one actual artifact, not a reproducible hash guarantee.

Source synopsis SHA-256 observed during read-only analysis: `0E4E55DE0B2ACFDA1CE0AE5C55749DC9938BE3C5637640D947FCD8A405B9BC21`. No source document/synced files were written.

**Limits of this evidence:** no WAV/audio, AES, LSB, TCP, Swing, SNR, recording, LAN or end-to-end implementation was tested; those are future phases. No audio benchmark values or screenshots are claimed. The Phase 1 foundation is complete; final project acceptance is not yet met.
