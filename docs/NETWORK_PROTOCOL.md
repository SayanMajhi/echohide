# TCP file transfer protocol version 1

Phase 1 contract; implemented and tested in Phase 5. One file per connection, one acknowledgement. Use `Socket`, `ServerSocket`, `DataInputStream` and `DataOutputStream`. All integers use Java data-stream **big-endian** encoding. No `writeUTF` modified UTF-8; filenames use explicit UTF-8 byte length.

| Order | Encoding | Value |
|---|---|---|
| 1 | 4 bytes | ASCII `EHTF` |
| 2 | unsigned byte | Version 1 |
| 3 | signed long | WAV file byte length, 1–configured limit, hard cap 104,857,600 |
| 4 | unsigned short | Filename UTF-8 byte length, 1–128 |
| 5 | exact filename bytes | Display name, decoded with strict UTF-8 validation |
| 6 | 32 raw bytes | Expected SHA-256 of the file |
| 7 | exact declared file bytes | Stream in bounded buffers |
| 8 | sender half-closes output | `shutdownOutput()` after flush; EOF terminates request |
| 9 | receiver reply | ASCII `EHAK` (4 bytes), status (1 byte), actual length (long), digest (32 bytes) |

ACK status 0 means success; 1 malformed request; 2 integrity mismatch; 3 storage failure. On failure, actual length and digest are all zero. For malformed/truncated input the peer may close before a reply; sender must handle missing ACK as failure. Never expose internal paths or exception text over the wire. Sender validates ACK magic, status, declared length and digest before reporting success. Check file size/hash before and during send; mutation during sending fails safely.

Receiver validates every field before allocating or creating the final file. Name is treated only as untrusted display metadata: reject path separators, control characters, `.`/`..` and malformed UTF-8; never join it to a destination path. Generate a UUID-based `.wav` filename in the configured receive directory. Create a temporary file in that directory with `CREATE_NEW`; compute SHA-256 as bytes stream to disk. Loop on reads until exactly the declared size, then require EOF after sender half-close; reject early EOF or extra bytes. Use bounded timeouts for EOF and all reads.

After EOF and matching digest, close output and move the temporary file to its new final name without replacement. Prefer atomic move where supported; otherwise a no-replacement move on the same filesystem is acceptable. Only then send success ACK. Delete a failed transfer's temporary file in `finally`, close sockets/streams, and preserve existing user files. If ACK delivery fails after publishing, receiver may retain a valid file while sender reports failure; retries can create a second distinct file. This is documented behavior, not exactly-once delivery.

Default connect timeout: 5 s. Socket read timeout: 15 s (inactivity, not total duration). Receiver will enforce a 120 s total transfer deadline in addition to read timeout, preventing indefinite slow transfers. Socket writes have no standard `SO_TIMEOUT`; a scheduled deadline must close the active socket to unblock a blocked write/read. Cancel closes sockets. Listening accept can block until stop closes the server socket. Production port range: 1–65535, default 5050; tests bind port 0 and discover the assigned port. Default bind is loopback; LAN use explicitly binds the appropriate interface and may require Windows firewall permission.

Progress reports actual bytes handled / declared total; reaching 100% sent bytes is not confirmed receipt. Transfer duration uses `System.nanoTime()` from immediately before connect through verified ACK at sender; receiver duration starts on accept and ends after publishing. Store these as separate measurements.

TCP provides an ordered byte stream with retransmission and congestion/flow control while the connection works. It does not preserve application writes as messages and cannot ensure that an interrupted connection eventually completes. A header read or file chunk may arrive over several reads. Length fields, read loops, exact EOF handling, explicit ACK and integrity checks provide the application protocol.

SHA-256 here detects mismatched file bytes, but is sent without a secret and does not authenticate the peer against an active attacker. AES-GCM authenticates the hidden payload only. Filenames, audio and connection metadata remain visible. TLS and peer identity are future extensions.
