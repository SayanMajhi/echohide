package com.sayan.echohide.stego;

import com.sayan.echohide.audio.WavAudio;
import com.sayan.echohide.crypto.AuthenticationFailedException;
import com.sayan.echohide.crypto.CryptoManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Core engine for Least Significant Bit (LSB) embedding and extraction in 16-bit PCM audio.
 */
public class LsbSteganography {

    public static class EmbeddingResult {
        private final WavAudio stegoAudio;
        private final int payloadBytes;
        private final int totalCarrierSamples;
        private final int samplesUsed;
        private final int bitsPerSample;
        private final double capacityUsagePercentage;

        public EmbeddingResult(WavAudio stegoAudio, int payloadBytes, int totalCarrierSamples,
                               int samplesUsed, int bitsPerSample, double capacityUsagePercentage) {
            this.stegoAudio = stegoAudio;
            this.payloadBytes = payloadBytes;
            this.totalCarrierSamples = totalCarrierSamples;
            this.samplesUsed = samplesUsed;
            this.bitsPerSample = bitsPerSample;
            this.capacityUsagePercentage = capacityUsagePercentage;
        }

        public WavAudio getStegoAudio() {
            return stegoAudio;
        }

        public int getPayloadBytes() {
            return payloadBytes;
        }

        public int getTotalCarrierSamples() {
            return totalCarrierSamples;
        }

        public int getSamplesUsed() {
            return samplesUsed;
        }

        public int getBitsPerSample() {
            return bitsPerSample;
        }

        public double getCapacityUsagePercentage() {
            return capacityUsagePercentage;
        }
    }

    /**
     * Calculates the maximum payload bytes that can be embedded into the given carrier audio.
     */
    public static int calculateMaxPayloadBytes(int totalSamples, int bitsPerSample) {
        int availableSamples = totalSamples - StegoHeader.HEADER_SAMPLES_COUNT;
        if (availableSamples <= 0) return 0;
        int totalPayloadBits = availableSamples * bitsPerSample;
        int totalCiphertextBytes = totalPayloadBits / 8;
        // GCM tag takes 16 bytes
        return Math.max(0, totalCiphertextBytes - 16);
    }

    /**
     * Embeds a secret text message into the carrier audio with AES-GCM encryption.
     *
     * @param carrierAudio   Original 16-bit PCM WAV audio
     * @param secretMessage  Secret text to hide
     * @param password       Passphrase for AES-GCM encryption
     * @param bitsPerSample  1, 2, or 4 LSBs per sample (default 1)
     * @return EmbeddingResult containing stego audio and embedding statistics
     */
    public static EmbeddingResult embed(WavAudio carrierAudio, String secretMessage, String password, int bitsPerSample)
            throws GeneralSecurityException, IllegalArgumentException {
        if (bitsPerSample != 1 && bitsPerSample != 2 && bitsPerSample != 4) {
            throw new IllegalArgumentException("bitsPerSample must be 1, 2, or 4 (received: " + bitsPerSample + ")");
        }

        // 1. Encrypt message with AES-256-GCM
        CryptoManager.EncryptedResult enc = CryptoManager.encrypt(secretMessage, password);
        byte[] ciphertext = enc.getCiphertext();
        byte[] salt = enc.getSalt();
        byte[] iv = enc.getIv();

        // 2. Prepare Header
        StegoHeader header = new StegoHeader(bitsPerSample, salt, iv, ciphertext.length);
        byte[] headerBytes = header.serialize();

        // 3. Check capacity
        int headerSamplesRequired = StegoHeader.HEADER_SAMPLES_COUNT; // 38 * 8 = 304 samples at 1-bit LSB
        int payloadBits = ciphertext.length * 8;
        int payloadSamplesRequired = (payloadBits + bitsPerSample - 1) / bitsPerSample;
        int totalSamplesRequired = headerSamplesRequired + payloadSamplesRequired;

        short[] samples = carrierAudio.getSamples();
        if (samples.length < totalSamplesRequired) {
            int maxBytes = calculateMaxPayloadBytes(samples.length, bitsPerSample);
            throw new IllegalArgumentException("Audio file is too short to embed message. Required: "
                    + totalSamplesRequired + " samples (" + (ciphertext.length + 38) + " bytes), Available: "
                    + samples.length + " samples (Max secret capacity: " + maxBytes + " bytes).");
        }

        // 4. Create copy of samples for stego audio
        short[] stegoSamples = Arrays.copyOf(samples, samples.length);

        // 5. Embed header at 1-bit LSB in the first 304 samples
        embedBytes(stegoSamples, 0, headerBytes, 1);

        // 6. Embed ciphertext at bitsPerSample LSBs starting after header
        embedBytes(stegoSamples, headerSamplesRequired, ciphertext, bitsPerSample);

        double usagePercent = ((double) totalSamplesRequired / samples.length) * 100.0;
        WavAudio stegoAudio = carrierAudio.withSamples(stegoSamples);

        return new EmbeddingResult(stegoAudio, secretMessage.getBytes().length, samples.length,
                totalSamplesRequired, bitsPerSample, usagePercent);
    }

    /**
     * Extracts and decrypts the secret text message from stego audio.
     *
     * @param stegoAudio Stego audio containing hidden message
     * @param password   Passphrase for decryption
     * @return Recovered plaintext message
     */
    public static String extract(WavAudio stegoAudio, String password)
            throws IllegalArgumentException, AuthenticationFailedException, GeneralSecurityException {
        short[] samples = stegoAudio.getSamples();
        if (samples.length < StegoHeader.HEADER_SAMPLES_COUNT) {
            throw new IllegalArgumentException("Audio is too short to contain a valid EchoHide header.");
        }

        // 1. Extract header bytes (38 bytes from first 304 samples at 1-bit LSB)
        byte[] headerBytes = extractBytes(samples, 0, StegoHeader.HEADER_BYTES_COUNT, 1);
        StegoHeader header = StegoHeader.deserialize(headerBytes);

        // 2. Validate ciphertext sample requirements
        int bitsPerSample = header.getBitsPerSample();
        int ciphertextLength = header.getCiphertextLength();
        int payloadBits = ciphertextLength * 8;
        int payloadSamplesRequired = (payloadBits + bitsPerSample - 1) / bitsPerSample;
        int totalSamplesRequired = StegoHeader.HEADER_SAMPLES_COUNT + payloadSamplesRequired;

        if (samples.length < totalSamplesRequired) {
            throw new IllegalArgumentException("Stego audio is corrupted or truncated. Expected "
                    + totalSamplesRequired + " samples, found " + samples.length);
        }

        // 3. Extract ciphertext bytes
        byte[] ciphertext = extractBytes(samples, StegoHeader.HEADER_SAMPLES_COUNT, ciphertextLength, bitsPerSample);

        // 4. Decrypt and authenticate with AES-GCM
        return CryptoManager.decrypt(ciphertext, password, header.getSalt(), header.getIv());
    }

    /**
     * Embeds an array of bytes into audio samples starting at sampleOffset using k bits per sample.
     */
    private static void embedBytes(short[] samples, int sampleOffset, byte[] data, int bitsPerSample) {
        int sampleIdx = sampleOffset;
        int mask = (1 << bitsPerSample) - 1;

        for (byte b : data) {
            int unsignedByte = b & 0xFF;
            // Process bits from MSB to LSB in chunks of size bitsPerSample
            for (int shift = 8 - bitsPerSample; shift >= 0; shift -= bitsPerSample) {
                int chunk = (unsignedByte >> shift) & mask;
                int currentSample = samples[sampleIdx];
                int modified = (currentSample & ~mask) | chunk;
                samples[sampleIdx] = (short) modified;
                sampleIdx++;
            }
        }
    }

    /**
     * Extracts an array of bytes from audio samples starting at sampleOffset using k bits per sample.
     */
    private static byte[] extractBytes(short[] samples, int sampleOffset, int byteCount, int bitsPerSample) {
        byte[] data = new byte[byteCount];
        int sampleIdx = sampleOffset;
        int mask = (1 << bitsPerSample) - 1;

        for (int i = 0; i < byteCount; i++) {
            int currentByte = 0;
            for (int shift = 8 - bitsPerSample; shift >= 0; shift -= bitsPerSample) {
                int sampleVal = samples[sampleIdx];
                int chunk = sampleVal & mask;
                currentByte = (currentByte << bitsPerSample) | chunk;
                sampleIdx++;
            }
            data[i] = (byte) currentByte;
        }

        return data;
    }
}

