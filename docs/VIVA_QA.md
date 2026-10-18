# Technical viva questions and answers

These explain the agreed design. Phase 1 has not yet implemented the audio/crypto/network features discussed below.

1. **What problem does EchoHide investigate?** Whether short encrypted text can be placed in PCM audio samples, recovered exactly, and transferred as a file while retaining acceptable listening quality.
2. **How do encryption and steganography differ?** Encryption makes content unreadable without a key; steganography embeds information within a carrier. Combining them does not guarantee the carrier is undetectable.
3. **Why 16-bit PCM WAV?** It provides direct integer samples with lossless storage. The decoder can read the exact sample bits used by the embedder.
4. **Is WAV always PCM?** No. WAV is a container and may hold compressed or floating-point audio. EchoHide must validate its actual encoding and bit depth.
5. **What is a sample versus a frame?** A sample is one channel's amplitude at one instant. A stereo frame contains two samples. At 44,100 frames/s, stereo contains 88,200 samples/s.
6. **Which bit is changed?** Bit 0 of each 16-bit sample's low byte; for little-endian PCM it is the first byte. Header bytes are not samples.
7. **What does `(b & 0xFE) | bit` do?** It clears the low bit and then sets it to 0 or 1. The other seven low-byte bits stay unchanged, and the sample's high byte is untouched.
8. **How does Java's signed byte affect bit operations?** Java promotes a byte to a sign-extended int. Use masks such as `b & 0xFF` when interpreting unsigned bytes; cast the masked result back when storing.
9. **Can changing the LSB overflow a signed 16-bit sample?** This bit replacement leaves the sign bit and other bits unchanged, so the sample remains in range. Its numeric value changes by at most one.
10. **How is capacity calculated?** One bit per sample gives `floor(sampleCount/8)` envelope bytes. Subtract 58 bytes for this version's header/tag to find the maximum UTF-8 message length, provided even the empty envelope fits.
11. **Why use UTF-8 byte length rather than String.length()?** Java string length counts UTF-16 code units. UTF-8 may require several bytes per character, so character count is not embedding capacity.
12. **How does extraction stay synchronized?** Both sides visit samples sequentially in the same interleaved order and use the same most-significant-bit-first payload bit ordering. The header defines ciphertext length.
13. **Can magic bytes prove a hidden message is real?** No. They are a parsing marker. Header bounds validate structure; successful GCM authentication establishes payload authenticity under the supplied password-derived key.
14. **Why can MP3 destroy the message?** Lossy compression reconstructs sample values instead of preserving exact PCM bits. Resampling or normalization can also modify the hidden LSBs.
15. **What does AES-256-GCM provide?** AES encryption with a 256-bit key plus authenticated integrity using GCM. Decryption succeeds only if the authentication tag verifies with the correct key, nonce and AAD.
16. **Why not directly use the password as the AES key?** Passwords have variable length and often low entropy. PBKDF2 stretches them into a fixed-length key using a salt and a deliberate work factor; a weak password remains vulnerable to guessing.
17. **What is the difference between salt and nonce?** Salt randomizes password key derivation. A GCM nonce identifies an encryption invocation and must not repeat under the same key. Neither needs to be secret.
18. **What is AAD?** Additional authenticated data is verified with the ciphertext tag but is not encrypted. Here it covers the public versioned header, salt and nonce in a canonical byte layout.
19. **Can the app distinguish wrong password from ciphertext tampering?** A GCM tag failure can result from either. The app reports authentication failure and must not claim a unique cause or display partial plaintext.
20. **Why cap iteration counts and declared lengths before decryption?** They come from untrusted headers. Bounds prevent excessive CPU work, huge allocations, overflow and reading beyond the carrier.
21. **What does TCP guarantee?** An ordered reliable byte stream while the connection succeeds, with retransmission and flow/congestion control. A failed connection may still leave an incomplete application transfer.
22. **Why is application framing required over TCP?** A receiver can read fewer bytes than a sender wrote, or combine multiple writes in one read. Explicit lengths and exact read loops identify fields and the file body.
23. **What do DataInputStream and DataOutputStream add?** They encode/decode fixed Java primitive representations, including big-endian integers. They do not create TCP message boundaries or validate lengths automatically.
24. **Why include SHA-256 if TCP already checks transport errors?** It compares the application's complete source/received file bytes and helps detect framing/storage mismatches. An unauthenticated digest cannot protect against an attacker who can replace both file and digest.
25. **When may the sender report success?** After a valid receiver ACK confirms matching file length/digest and successful publication. Writing all bytes locally does not prove receipt.
26. **Why use temporary files and generated receive names?** Temporary files prevent incomplete transfers appearing complete; generated names avoid using attacker-controlled paths and replacing existing files.
27. **What does try-with-resources do?** It closes AutoCloseable resources on normal or exceptional exit. Listener lifecycle still needs explicit stop/close to unblock waiting network operations.
28. **Why is SwingWorker useful?** Long KDF, audio/file I/O and network tasks cannot run on Swing's event dispatch thread without freezing the interface. Workers run background tasks and deliver updates safely.
29. **How is SNR measured?** Compare original/stego PCM samples: `10*log10(sum(original²)/sum((original-stego)²))`, with documented silent/zero-noise edge cases. File header or compressed byte differences are not audio SNR.
30. **How do you demonstrate recovery accuracy?** Compare recovered and original UTF-8 bytes exactly after authenticated decryption, and report successful cases over attempted valid cases. Visual similarity or playback alone is insufficient.
31. **Why use System.nanoTime() for duration?** It measures elapsed time without wall-clock adjustments. Store differences and convert to milliseconds; the absolute value is not a timestamp.
32. **How are tests repeatable with secure randomness?** Generate deterministic carrier/message fixtures and assert recovery/properties, not fixed ciphertext. Salt/nonce stay unpredictable in production crypto.
33. **Why are mono and stereo tests both needed?** Channels change frame alignment and sample count. Testing both catches code that treats frames as individual samples or skips a channel.
34. **Is EchoHide a secure messenger or live call system?** The scope is a local file steganography prototype with a TCP extension. Peer identity, TLS, live streaming and traffic concealment are outside the present scope.
