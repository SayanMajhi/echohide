# EchoHide: Audio Steganography with Authenticated AES-GCM and TCP File Transfer
**Academic Project Report & System Documentation**

---

## 1. Executive Summary
**EchoHide** is an end-to-end audio steganography system developed in Java. It allows an authorized sender to embed confidential text messages within standard 16-bit PCM uncompressed WAV audio files. The secret message is secured using **AES-256-GCM** authenticated encryption with PBKDF2 key derivation. The resulting steganographic audio file can be verified for acoustic imperceptibility through **Signal-to-Noise Ratio (SNR)** and **Peak Signal-to-Noise Ratio (PSNR)** metrics, transmitted over an IP network using a custom **length-framed TCP socket protocol**, and extracted exclusively by authorized recipients holding the correct cryptographic secret.

---

## 2. System Architecture & Methodology

```
+-----------------------------------------------------------------------------------+
|                                  SENDER SIDE                                      |
+-----------------------------------------------------------------------------------+
|  [Secret Message]  +  [Passphrase]                                                |
|          |                                                                        |
|          v                                                                        |
|  [PBKDF2-HMAC-SHA256 Key Derivation (65,536 iters)]                               |
|          |                                                                        |
|          v                                                                        |
|  [AES-256-GCM Encryption] ----> Generates 128-bit Auth Tag + 12-byte IV           |
|          |                                                                        |
|          v                                                                        |
|  [38-Byte Stego Header Assembly] (Magic 'ECHO', Version, Bits/Sample, Salt, IV, L) |
|          |                                                                        |
|          v                                                                        |
|  [16-Bit PCM WAV Carrier Samples]                                                 |
|          |                                                                        |
|          v                                                                        |
|  [LSB Embedding Engine]                                                           |
|    - First 304 samples: 38-byte Header at 1-bit LSB                              |
|    - Subsequent samples: Encrypted Payload at k-bit LSB (1, 2, or 4)              |
|          |                                                                        |
|          v                                                                        |
|  [Lossless Stego-WAV Output]                                                      |
+-----------------------------------------------------------------------------------+
                                           |
                                           v
                       +---------------------------------------+
                       |   Length-Framed TCP Socket Transfer   |
                       |   (Magic, Name, Length, Stream, ACK)  |
                       +---------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                                 RECEIVER SIDE                                     |
+-----------------------------------------------------------------------------------+
|  [Received Stego-WAV File]                                                        |
|          |                                                                        |
|          v                                                                        |
|  [LSB Extraction Engine]                                                          |
|    - Read first 304 samples -> Deserialize Header -> Validate 'ECHO' Magic        |
|    - Extract Ciphertext + Salt + IV according to recorded bits/sample             |
|          |                                                                        |
|          v                                                                        |
|  [AES-256-GCM Decryption & Authenticated Tag Check]                               |
|    - If tag matches & key valid: Output original plaintext                        |
|    - If corrupted or wrong key: Throw AuthenticationFailedException               |
+-----------------------------------------------------------------------------------+
```

---

## 3. Mathematical Foundations

### 3.1 Least Significant Bit (LSB) Modification
Let $s_i \in [-32768, 32767]$ represent the $i$-th 16-bit audio sample. For $k$-bit LSB embedding ($k \in \{1, 2, 4\}$):
$$\text{mask} = 2^k - 1$$
$$\hat{s}_i = (s_i \ \& \ \sim\text{mask}) \ | \ c_i$$
where $c_i$ is the $k$-bit chunk of data to embed.

The maximum sample amplitude disturbance is:
$$|\Delta s| \le 2^k - 1$$
For $k=1$, $|\Delta s| \le 1$. Relative to the full dynamic range of $65,536$, the maximum perturbation is only:
$$\frac{1}{65536} \approx 0.0015\%$$

### 3.2 Mean Squared Error (MSE)
Given $N$ samples of original carrier $s$ and stego signal $\hat{s}$:
$$\text{MSE} = \frac{1}{N} \sum_{i=0}^{N-1} (s_i - \hat{s}_i)^2$$

### 3.3 Signal-to-Noise Ratio (SNR)
$$\text{SNR (dB)} = 10 \cdot \log_{10} \left( \frac{\sum_{i=0}^{N-1} s_i^2}{\sum_{i=0}^{N-1} (s_i - \hat{s}_i)^2} \right)$$

### 3.4 Peak Signal-to-Noise Ratio (PSNR)
For 16-bit signed audio, the peak sample value is $\text{MAX} = 32,767$:
$$\text{PSNR (dB)} = 10 \cdot \log_{10} \left( \frac{32767^2}{\text{MSE}} \right) = 20 \cdot \log_{10} \left( \frac{32767}{\sqrt{\text{MSE}}} \right)$$

---

## 4. Empirical Benchmark Data

All benchmarks conducted using a 5.0-second 44.1 kHz stereo carrier (441,000 samples) on Java 21:

| Payload Size | LSB Mode | SNR (dB) | PSNR (dB) | MSE | Embed Time | Extract Time | Integrity Result |
|---|---|---|---|---|---|---|---|
| **32 Bytes** | 1 LSB | 103.59 dB | 121.29 dB | 0.0008 | 30 ms | 30 ms | PASS (100% Match) |
| **128 Bytes** | 1 LSB | 100.35 dB | 118.05 dB | 0.0017 | 28 ms | 29 ms | PASS (100% Match) |
| **512 Bytes** | 1 LSB | 95.52 dB | 113.22 dB | 0.0051 | 34 ms | 26 ms | PASS (100% Match) |
| **2,048 Bytes** | 1 LSB | 89.85 dB | 107.55 dB | 0.0189 | 32 ms | 27 ms | PASS (100% Match) |
| **8,192 Bytes** | 1 LSB | 83.88 dB | 101.58 dB | 0.0747 | 34 ms | 34 ms | PASS (100% Match) |
| **32 Bytes** | 2 LSB | 100.87 dB | 118.57 dB | 0.0015 | 34 ms | 36 ms | PASS (100% Match) |
| **128 Bytes** | 2 LSB | 96.79 dB | 114.49 dB | 0.0038 | 43 ms | 52 ms | PASS (100% Match) |
| **512 Bytes** | 2 LSB | 91.78 dB | 109.48 dB | 0.0121 | 25 ms | 29 ms | PASS (100% Match) |
| **2,048 Bytes** | 2 LSB | 85.92 dB | 103.62 dB | 0.0467 | 24 ms | 20 ms | PASS (100% Match) |
| **8,192 Bytes** | 2 LSB | 79.86 dB | 97.56 dB | 0.1882 | 20 ms | 20 ms | PASS (100% Match) |

---

## 5. Security & Cryptographic Analysis
1. **Confidentiality**: Provided by AES-256 with counter mode (GCM). An eavesdropper extracting the raw LSB bits will find pseudo-random ciphertext indistinguishable from white noise.
2. **Authenticity & Integrity**: Provided by GCM's 128-bit authentication tag calculated via GHASH. If any audio sample carrying payload bits is modified or corrupted during network transit, or if an incorrect decryption key is supplied, `Cipher.doFinal()` throws `AEADBadTagException`.
3. **Key Hardening**: Passphrases are converted to 256-bit keys via PBKDF2 with HMAC-SHA256 running 65,536 iterations and unique 16-byte random salts to defeat rainbow tables and brute-force dictionary attacks.
4. **Replay Protection**: Each encryption operation uses a fresh cryptographically secure 96-bit IV generated by `java.security.SecureRandom`.

---

## 6. TCP Transmission Protocol Specification
The TCP file transfer component utilizes a length-framed protocol:
- **Magic Marker (4 bytes)**: `0x45 0x43 0x48 0x4F` (`ECHO`)
- **Filename Length (2 bytes)**: Big-endian unsigned 16-bit integer
- **Filename (variable)**: UTF-8 encoded string
- **File Size (8 bytes)**: Big-endian signed 64-bit long
- **File Stream**: Transferred in 8,192-byte blocks
- **Acknowledgment (1 byte)**: Receiver sends `0x06` (ACK) upon flush and close

---

## 7. Conclusion
The EchoHide project demonstrates that 16-bit PCM WAV audio is an exceptional carrier for secure steganographic data communication. Audio quality degradation remains below acoustic detection thresholds ($SNR > 79 \text{ dB}$ across all scenarios), cryptographic authenticity ensures zero unauthorized decryption, and length-framed TCP transmission facilitates reliable cross-computer stego-file exchange.

