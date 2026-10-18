# Version 1 hidden payload

Design only in Phase 1. One sample carries one payload bit. Process interleaved samples in file order (left, right, left, right for stereo), with payload bytes most-significant bit first. For each little-endian 16-bit sample, modify bit 0 of its first byte: `(lowByte & 0xFE) | payloadBit`. Leave the high byte untouched. Sample values including -32768, -1, 0 and 32767 remain valid; each changes by at most one quantization unit. Decoder visits exactly the same sample order; no external position map is needed.

## Envelope layout

All integer fields below are **big-endian**, independent of little-endian WAV samples. Use unsigned interpretations where stated and validate using `long` before allocating Java arrays.

| Offset | Bytes | Field |
|---|---|---|
| 0 | 4 | ASCII `EHLS` magic |
| 4 | 1 | Version `1` |
| 5 | 1 | Algorithm suite `1`: AES-256-GCM / PBKDF2-HMAC-SHA256 |
| 6 | 4 | PBKDF2 iteration count, positive signed int, 600,000–2,000,000 |
| 10 | 4 | Ciphertext length including 16-byte GCM tag, positive signed int, at least 16 |
| 14 | 16 | Random salt |
| 30 | 12 | Random nonce |
| 42 | L | Ciphertext followed by 16-byte authentication tag |

Canonical additional authenticated data (AAD) is exactly bytes 0 through 41, including salt and nonce. Both crypto and codec use one canonical encoder. Header fields are public, but GCM authenticates them on successful decryption. Ciphertext length is known before encryption: UTF-8 plaintext byte length + 16. No text is displayed before tag verification. Generate salt/nonce with `SecureRandom` for every encryption; never reuse a nonce with the same key.

Reject unsupported versions/suites, bad magic, invalid iterations, length <16, lengths above carrier capacity or file cap, arithmetic overflow and trailing bytes in a supplied serialized envelope. During audio extraction, untouched samples after the exact envelope are normal and are not serialized trailing bytes. Check header and length bounds **before** allocating ciphertext or performing expensive PBKDF2. Structural validity does not prove a payload is authentic.

## Capacity

Let `N = frameCount * channels = pcmByteLength / 2`. Raw payload capacity is `floor(N / 8)` bytes. Envelope overhead is `42 + 16 = 58` bytes, so maximum UTF-8 message bytes are `floor(N / 8) - 58` **when raw capacity >=58**. A smaller carrier cannot even hold an empty authenticated message; the UI must say "insufficient carrier" rather than implying that a zero-byte message fits.

For a message of `P` UTF-8 bytes, embedding uses `8 * (P + 58)` samples. Utilization is `100 * (P + 58) / floor(N / 8)`. Example arithmetic, not a benchmark: ten seconds of 44,100 Hz mono has 441,000 samples, 55,125 raw bytes and 55,067 message bytes. Stereo doubles the available samples for equal duration/sample rate.

One LSB changes amplitude slightly without changing sample count or playback duration. This often has low audible impact, but listening and SNR must measure that claim. Compression, resampling, normalization and sample edits can change LSBs, causing header or authentication failure. MP3 is excluded because lossy encoding reconstructs different sample values. Encryption protects content; LSB hiding does not establish undetectability.

## Password parameters

Default PBKDF2 work factor is 600,000 with 256 derived key bits, 16-byte salt, 12-byte GCM nonce and 128-bit tag. The 600,000 baseline follows [OWASP's PBKDF2-HMAC-SHA256 guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html); applying it here is a design choice for password-derived encryption keys. Measure latency on the demonstration machine before finalization. The 2,000,000 upper bound is a local resource policy against hostile headers, not a cryptographic maximum. A strong passphrase is still necessary. Lower-work-factor fixtures must not weaken the production decoder's accepted range.
