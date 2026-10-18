# Testing strategy and acceptance matrix

## Executable now

From the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\maven.ps1 clean verify
java -jar .\target\echohide-0.1.0-SNAPSHOT.jar --check
java -jar .\target\echohide-0.1.0-SNAPSHOT.jar --help
```

`ProjectConfigTest` exercises bundled resource loading, port limits, KDF work-factor limits, timeout limits, file-size cap, numeric overflow, missing/blank settings and accepted boundary settings. Parameterized invocations count individually. Read [verification](VERIFICATION.md) for actual results, not this planned matrix. Configuration tests do not establish encryption, audio or transfer correctness.

## Later required tests

| Phase / test file | Cases and expected behavior |
|---|---|
| 2 `audio/WavFileServiceTest` | Generated mono and stereo fixtures round-trip format, PCM and ancillary metadata. Reject non-WAV, truncated/oversized chunks, misaligned frames, unsupported bit depth/format/channels. Existing destination is unchanged on refused save. |
| 3 `crypto/AesGcmServiceTest` | Empty, short and Unicode UTF-8 messages recover exactly. Wrong password, modified tag/ciphertext/AAD fail without plaintext. Repeated encryption uses different salt/nonce. Reject malformed parameters before expensive derivation. |
| 4 `steganography/PayloadCodecTest` | Known header byte layout; bad magic/version/suite; negative, short and impossible ciphertext lengths; malicious iteration counts; truncated data and serialized trailing bytes rejected. |
| 4 `steganography/LsbSteganographyTest` | Exact encrypted bytes after embed/extract; mono/stereo; -32768, -1, 0, 1, 32767; every sample changes by at most 1; untouched samples/chunks preserved; full supported payload fits; one byte over fails without changing source. Too-small carrier cannot embed even empty message. |
| 5 `network/TcpFileTransferTest` | Ephemeral loopback server, exact file bytes and SHA-256, verified ACK. Fragment header/body writes deliberately. Reject short/extra declared lengths, wrong digest, oversized/negative length, truncated header, path-like/invalid UTF-8 names, bad version. Interrupted connection/timeouts clean temp file; stop unblocks accept/read/write; no replacement of existing files. Malformed/missing/negative ACK fails sender. |
| 7 `metrics/AudioMetricsTest` | Analytically known SNR, identical signals, silence with/without noise, format mismatch, byte capacity and utilization, correctly escaped locale-independent CSV. |
| 8 `integration/EndToEndTest` | Generated original WAV → AES → embed → new WAV → real localhost sockets → read → extract → decrypt → exact UTF-8 verification; original/stego/received integrity as appropriate. |

Use seeded carrier samples and explicit fixture sizes; random cryptographic parameters stay random. No microphone, real sound device or external network is required for automated tests. Use bounded futures/latches and real ephemeral sockets, not arbitrary sleeps. Temporary files use JUnit `@TempDir`; server resources must close even when assertions fail. All `*Test` classes, including integration tests, run under Surefire with `mvn verify`; no separate unconfigured integration-test naming convention is assumed.

## Manual UI/device checks in Phase 6/8

Check correct/incorrect password, WAV import errors, original/stego playback, stop listener, occupied port, cancel transfer, progress semantics, save confirmation, local file decoding, Unicode and capacity display. Keep UI responsive during PBKDF2 and transfer. Run two processes on localhost; LAN demonstration uses a second machine when available and records any firewall setup. Screenshots must come from the actual running GUI. Listening is subjective evidence alongside SNR, not a substitute for exact recovery tests.
