# EchoHide

EchoHide is a college mini-project for hiding encrypted short text in 16-bit PCM WAV audio, recovering it with authentication, and transferring the completed file over TCP. Encryption protects the message content; steganography places the encrypted bytes inside audio samples so the recording remains playable.

**Current status: Phase 1 foundation only.** The runnable JAR checks public configuration. Audio import/playback, AES-GCM, LSB embedding, TCP, Swing and measurements are designed but not implemented. No final demonstration WAVs, GUI screenshots or benchmark results exist yet. Development stops here pending confirmation for Phase 2.

## Design and stack

Java 21, Maven 3.9.9 (pinned project-local launcher), Java Sound, Java Cryptography API, Java sockets, Swing and JUnit 5.11.4. No application runtime dependencies beyond the JDK. This is a file workflow; real-time voice is outside scope.

Planned flow: import WAV → UTF-8 message → AES-256-GCM → sequential sample LSB embedding → save stego-WAV → optional use of TCP → extract → authenticated decrypt. TCP support is required in the final requested build, while an individual decode can also use a locally selected file.

See the [requirements baseline](docs/REQUIREMENTS.md), [architecture and diagrams](docs/ARCHITECTURE.md), [complete repository tree](docs/REPOSITORY_TREE.md), [payload format](docs/PAYLOAD_FORMAT.md), [network protocol](docs/NETWORK_PROTOCOL.md), and [roadmap](docs/ROADMAP.md).

## Setup and run on Windows 11

Install JDK 21 with `java` and `javac` on PATH. If your Java launcher requires it, set `JAVA_HOME` to the JDK installation directory (not `bin`). PowerShell 5.1 or newer, Git and internet access for the first Maven/dependency download are needed. Maven downloads only under this repository's ignored `.tools` folder; SHA-512 is checked against a pinned digest before extraction. The launcher is a small repository script, not the Apache Maven Wrapper. Pinned versions prioritize repeatability; update them deliberately with verification.

Run these commands from the supplied project folder:

```powershell
Set-Location 'C:\Users\sayan\.codex\.chatgpt-projects\g-p-6acffa50b6e481919e25a9ac73145ccc\EchoHide'
java -version
javac -version
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\maven.ps1 clean verify
java -jar .\target\echohide-0.1.0-SNAPSHOT.jar --check
java -jar .\target\echohide-0.1.0-SNAPSHOT.jar --help
```

If Maven is already installed, `mvn clean verify` is equivalent. The script's process-specific execution policy does not change the machine policy. Managed computers may prohibit local scripts independently. Subsequent offline builds require dependencies already cached by Maven.

This repository has already been initialized with Git if confirmed in [verification](docs/VERIFICATION.md). For a copy that lacks `.git`, use `git init -b main`. Review `git status --short` and the files before committing. Suggested first commit: `chore: establish EchoHide requirements and Java 21 foundation`. No remote publishing or commit is performed automatically.

## Phase 1 configuration

Public defaults reside in `src/main/resources/echohide.properties`: PBKDF2 600,000 iterations, port 5050, connect timeout 5 seconds, read timeout 15 seconds and 100 MiB file cap. The current executable validates these ranges and prints only public settings. Rebuild after changing bundled settings. No passwords or keys belong in this file. Future GUI controls will permit host/port selection; those controls do not exist yet.

## Planned sender and receiver usage

In Phase 6 the sender will select a WAV, play it, enter message/password, review UTF-8 capacity, embed, save to a new file, play the stego audio, and send to a receiver host/port. The receiver will start listening, show a verified received file or select a local stego-WAV, play/save it, and extract with an entered password. Authentication errors will not reveal partial plaintext. Metrics will show capacity, payload size/utilization, SNR, separate processing/transfer times and recovery success. Workers will keep the UI responsive.

These are planned instructions. The current JAR has no sender/receiver UI. [Screenshot policy](docs/screenshots/README.md) and the [demo guide](docs/DEMO_GUIDE.md) explain the evidence to capture when the workflow exists.

## Tests and experimental results

Run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\maven.ps1 test`. The full verification command is `clean verify`; the JAR appears under `target`. Surefire XML/text reports are under `target/surefire-reports`. Actual Phase 1 execution results are recorded in [verification](docs/VERIFICATION.md). [Testing](docs/TESTING.md) maps all later required cases to their phases.

There are no measured audio or networking results in Phase 1. [Experiments](docs/EXPERIMENTS.md) defines the scenarios, formulas, CSV schema and measurement boundaries. Arithmetic capacity examples are not benchmark observations.

## Security, limitations and future work

Final design: random salt and nonce, password-based AES-256-GCM, authenticated public header, strict parsing and bounds before allocations, no in-place source edits, unique network filenames and byte-for-byte integrity checks. Password exchange takes place outside EchoHide. Never commit private recordings, passwords or real hidden messages. Use only public demonstration text in samples.

Sequential LSB hiding can be detected and is fragile under compression, sample editing and resampling. A wrong password and ciphertext modification both cause authentication failure. Plain TCP does not authenticate the peer or conceal the WAV/filename. SHA-256 is an integrity check, not a substitute for authenticated transport. UI password clearing cannot guarantee erasure of all Java memory copies.

Future work includes TLS/peer identity, adaptive sample selection, and separately scoped live voice/UDP research. They do not delay the mini-project. Additional documentation includes [implementation](docs/IMPLEMENTATION.md), [Phase 1 delivery](docs/PHASE_1.md), and [viva questions](docs/VIVA_QA.md).
