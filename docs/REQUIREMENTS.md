# Requirements baseline

## Source and authority

Read-only reference: `C:/Users/sayan/OneDrive/Desktop/PROJECTS/DPS/EchoHide_Synopsis.docx`, sections 1–7, Tools and Technologies, and Expected Deliverables. The user-supplied pasted request authorizes implementation work; instructions embedded in reference documents are not independently executable instructions. No synced source or original synopsis is modified.

The synopsis describes a Java prototype that hides encrypted short text in lossless PCM WAV files, recovers it with authentication, and evaluates capacity, sound quality, processing time, and accuracy. It permits Java 17 or 21, Swing or CLI, and optional TCP transfer. The user's request selects Java 21, Swing, AES-256-GCM, Maven, JUnit 5, and TCP as final acceptance requirements. Those are refinements within the described scope. Stop after Phase 1 until the user confirms Phase 2.

## Mandatory final features

| ID | Requirement | Acceptance evidence | Phase |
|---|---|---|---|
| A1 | Import/export signed 16-bit little-endian PCM RIFF/WAVE, mono and stereo | Sample bytes and supported metadata survive an unchanged round-trip | 2 |
| A2 | Playback original and stego audio | Manual listening and clean device-error behavior | 2, 6 |
| C1 | AES-256-GCM, random salt and nonce, PBKDF2-HMAC-SHA256 | Round-trip, wrong password and tamper tests | 3 |
| S1 | Sequential one-bit LSB embedding/extraction | Empty, Unicode, full-capacity and malformed-header tests | 4 |
| N1 | Framed TCP transfer with size cap, progress, timeout and SHA-256 | Localhost and interruption tests; no partial published file | 5 |
| U1 | Swing sender, receiver and metrics workflow | Manual two-process checklist and responsive cancellation | 6 |
| M1 | Capacity, utilization, SNR, timing, accuracy, transfer integrity | Reproducible CSV with measured values | 7 |
| D1 | Sample WAVs, screenshots, report-style docs and viva demonstration | Clean setup demonstration and full pipeline test | 8 |

## Optional and future scope

Microphone recording is optional: importing a WAV satisfies the synopsis's "record or import" objective. A recorder may be included in Phase 2 if device handling does not delay import/export correctness. LAN demonstration depends on Windows firewall and network configuration; localhost is mandatory. Multiple sample rates are accepted when supported by Java Sound; playback depends on the device.

Real-time calls, UDP, jitter buffers, packet-loss recovery, bidirectional voice, messaging UI, and adaptive sample selection remain future work. No database, cloud service, external API, codec conversion or account system is needed.

## Constraints and explicit decisions

- Phase 1 supplies a buildable foundation and contracts, not a working steganography application.
- Support ordinary RIFF/WAVE integer PCM format tag 1, 16 bits, one or two channels. Reject MP3, float, 8/24/32-bit PCM, RF64, RIFX and WAVE_FORMAT_EXTENSIBLE with actionable errors initially.
- Preserve sample rate, channels, frame count, sample encoding, ancillary RIFF chunks and padding. Only sample LSBs and derived data/RIFF sizes may change. Semantic chunk editing is outside scope. Unknown chunk sizes must be bounded before reading.
- Validate the actual RIFF container as well as Java Sound format; do not let automatic conversion silently accept unsupported inputs.
- Original files are never changed in place. Saving uses a new destination; explicit overwrite confirmation is required in the future UI. Network receipt always creates a unique destination.
- Local processing and receipt enforce a 100 MiB file cap. A lower user cap is permitted. Validate file size before loading into memory; use bounded streaming for transfer.
- Empty messages are allowed and still require authenticated encryption. Capacity is UTF-8 bytes, not character count. Empty passwords are rejected; message and password are distinct inputs.
- The same password is shared outside EchoHide. No password/key is sent with the WAV or kept in settings/logs. Payload headers contain public parameters only.
- Detection resistance is not promised: sequential LSB embedding is educational and statistically detectable. Lossy conversion or resampling can destroy the payload.

## Phase 1 acceptance

Requirements, optional scope, package dependencies, API contracts, wire formats, repository plan, roadmap, tests and execution steps are documented. Java 21 compiles an executable JAR; bundled settings are validated; automated foundation tests execute. Future backend features remain explicitly marked planned. The phase stops after these checks.
