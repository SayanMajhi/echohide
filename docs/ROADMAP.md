# Development roadmap

Estimates are active engineering time for implementation, debugging, verification and explanation on a working Windows/JDK setup. They are planning estimates, not recorded work or delivery commitments. College review and learning time may add several days.

| Phase | Goal and principal files | Acceptance before proceeding | Estimate | Suggested Git commit |
|---|---|---|---|---|
| 1 | Requirements, formats, documentation, Maven, entry point and config | Foundation builds, config tests run, scope is explicit | 2–4 h | `chore: establish EchoHide requirements and Java 21 foundation` |
| 2 | `audio/*`, `model/PcmAudio`, audio exception and WAV fixture/tests | Mono/stereo lossless round-trip, metadata preserved, unsupported/corrupt WAV rejected, playback checked | 4–6 h | `feat: add validated PCM WAV import export and playback` |
| 3 | `crypto/*`, encrypted model/auth exception and crypto tests | Empty/Unicode round-trip; random salt/nonce; wrong password and tampering fail | 3–5 h | `feat: add password-derived authenticated AES encryption` |
| 4 | `steganography/*`, embedding model/exceptions and tests | Exact boundary capacity, header rejection, signed sample boundaries, encrypted round-trip | 4–6 h | `feat: embed and extract versioned LSB audio payloads` |
| 5 | `network/*`, hashing, transfer model/exception and tests | Localhost exact file and ACK, interrupts/timeouts/unsafe framing leave no published partial file | 5–7 h | `feat: add framed TCP file transfer with verified receipt` |
| 6 | `ui/*`, update entry point, screenshots | Two processes complete workflow, playback and errors work, stop/cancel responsive | 5–8 h | `feat: integrate Swing sender receiver and metrics views` |
| 7 | `metrics/*`, result model, CSV/evaluation script/tests | Repeatable measured CSV; analytic SNR/capacity checks; no invented values | 3–5 h | `feat: add reproducible audio and transfer evaluation` |
| 8 | Integration tests, generated sample assets, demo script, final documentation | Clean build, full pipeline, manual UI/LAN evidence and 5–8 minute demo | 4–6 h | `docs: finalize verified EchoHide demonstration and report` |

Total: approximately 30–47 engineering hours. Optional recorder: add 2–3 h. Later phases must each report goal, exact changed paths, complete code, explanation, commands, expected/observed tests, checklist and suggested commit; update the current-status and verification documents with evidence. After Phase 1, wait for the user's confirmation before any Phase 2 implementation.
