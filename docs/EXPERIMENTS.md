# Reproducible evaluation plan

Phase 1 contains no measured audio or transfer values. Implementation, CSV export and real measurements arrive in Phase 7. Do not populate a results table with illustrative numbers.

## Formulas and units

For original signed PCM sample `x[i]` and modified sample `y[i]`, accumulate in `double` to avoid integer overflow: signal sum `S = sum(x[i] * (double)x[i])`; noise sum `E = sum((x[i] - (double)y[i])^2)`. `SNR_dB = 10 * log10(S/E)`. Dividing both sums by N gives the equivalent ratio of mean powers. Identical signals (`E=0`) return positive infinity, including silent identical signals by explicit convention. Silence modified to nonzero noise (`S=0, E>0`) returns negative infinity. Reject empty inputs and mismatched sample formats/counts. Interpret infinite values with these edge cases in mind.

Raw capacity `C = floor(frameCount * channels / 8)`; authenticated message capacity `C - 58` if `C >=58`; otherwise unsupported. Payload bytes include the 42-byte header and 16-byte GCM tag. Utilization `% = 100 * payloadBytes / C`; report no utilization when C is zero. Recovery success is exact equality of the original and recovered UTF-8 byte arrays after authenticated decryption. Accuracy `% = 100 * successfulCases / attemptedCases`; distinguish expected negative tests from valid recovery experiments.

Elapsed milliseconds `= (System.nanoTime() end - start) / 1_000_000.0`. Record encryption/KDF, embedding, extraction, decryption/KDF separately. Embedding excludes encryption and WAV disk I/O; extraction excludes decryption and WAV disk I/O. Sender TCP time includes connect through verified ACK; receiver time has its own boundaries. SHA-256 compares the saved stego-WAV before transmission to the received file, not the original carrier to the stego carrier (which intentionally differ).

## Scenarios

Generate deterministic 44,100 Hz 16-bit PCM mono/stereo carriers at 1, 5, 10 and 30 seconds with a fixed fixture seed. Use public ASCII messages of exactly 0, 16, 128, 1,024 bytes and maximum fitting capacity for each carrier, plus fixed Unicode text with actual UTF-8 length recorded. A separate one-byte-over-capacity test must fail and is excluded from successful-recovery averages. Synthetic signals make results reproducible; optionally add a licensed or self-recorded speech/music set, labeled separately.

Run 3 unreported warm-ups then 10 measured repetitions per valid scenario. Use production KDF settings. Keep each raw row; aggregate median and range (include KDF separately). Do not pretend nanosecond timer resolution removes OS/JIT/device variability. Encryption randomness changes which LSBs flip, so bit-identical stego output and identical SNR across repetitions are not expected.

## CSV schema

`run_id,timestamp_utc,java_version,os,cpu,carrier_seed,sample_rate,channels,duration_s,message_utf8_bytes,payload_bytes,capacity_bytes,utilization_percent,pbkdf2_iterations,snr_db,encryption_ms,embedding_ms,extraction_ms,decryption_ms,tcp_sender_ms,tcp_receiver_ms,recovery_success,sha256_match,status`

Use UTF-8 CSV with a header, dot decimal separator and proper quoting. Represent infinities as `Infinity`/`-Infinity`; absent transfer values remain blank, not zero. Retain failed-case status without logging message/password/key content. File output must refuse existing results unless the user explicitly chooses a new run or approves replacement. Publish machine/JDK settings alongside the measured rows under `results/`.
