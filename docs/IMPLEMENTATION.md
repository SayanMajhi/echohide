# Implementation guide

## Implemented in Phase 1

`pom.xml` targets Java 21, adds JUnit only in test scope, pins build plugins and packages an executable entry point. `EchoHide.java` supplies help and a public configuration check. `ProjectConfig.java` is an immutable record that reads classpath settings, validates ports, iteration count, finite positive timeouts and a bounded file size, and reports missing/malformed configuration as `IOException`. The loader leaves caller-owned streams open; the classpath loader closes its own stream. The PowerShell Maven launcher downloads a pinned distribution, checks SHA-512, and invokes it from the project root. Complete compilable code is in these actual repository files, not pseudocode embedded in the guide.

## Audio and LSB plan

The RIFF parser must validate chunk lengths with overflow-safe arithmetic, honor odd-length padding, enforce whole PCM frames and preserve ancillary chunks/order. Java Sound supplies playback/optional recording and format validation; a bounded RIFF representation handles metadata preservation that a generic Java Sound re-export may otherwise discard. Do not promise metadata preservation merely because samples play correctly.

Embed a payload bit by masking the low byte of a signed 16-bit sample. This works identically for positive/negative values and mono/stereo because the sample encoding is fixed. Both sides iterate interleaved samples from the beginning, and the fixed header provides exact payload boundaries. Validate that the entire envelope fits before modifying a copy. Never change a WAV header as though it were audio samples.

## Cryptography plan

Convert messages explicitly with UTF-8. Use `PBKDF2WithHmacSHA256`, `AES/GCM/NoPadding`, `SecretKeySpec` and `GCMParameterSpec(128, nonce)`. Derive 256 bits with a new salt, generate a fresh nonce, and authenticate the canonical header with `updateAAD`. Decode only the bounded header before authentication; retain no displayed plaintext until `doFinal` verifies the tag. Map `AEADBadTagException` to a clear authentication error. Avoid arbitrary plaintext/password length limits hidden in the UI; enforce message byte capacity and document any chosen password resource limit.

## Networking and UI plan

Use exact-length read loops; neither `read` nor `write` defines a TCP message. Stream files instead of allocating declared file size. Receiver uses generated destinations, temporary files, SHA-256 and a verified acknowledgement. UI work runs in background workers; all visible component updates run on the Swing event dispatch thread. Separate "bytes sent" from "receiver confirmed" status. No backend implementation is supplied in Phase 1.

See [payload format](PAYLOAD_FORMAT.md), [network protocol](NETWORK_PROTOCOL.md) and [architecture API contracts](ARCHITECTURE.md) for exact layouts and responsibilities.
