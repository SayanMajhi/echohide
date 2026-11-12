package com.sayan.echohide.stego;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Encapsulates the metadata header embedded in the audio file.
 * Total size: 38 bytes
 * - Magic (4 bytes): 'ECHO'
 * - Version (1 byte): 1
 * - Bits per sample (1 byte): 1..4
 * - Salt (16 bytes): PBKDF2 salt
 * - IV (12 bytes): AES-GCM IV
 * - Payload length (4 bytes): int32 ciphertext length in bytes
 */
public class StegoHeader {

    public static final byte[] MAGIC = new byte[]{'E', 'C', 'H', 'O'};
    public static final byte VERSION = 1;
    public static final int HEADER_BYTES_COUNT = 4 + 1 + 1 + 16 + 12 + 4; // 38 bytes
    public static final int HEADER_SAMPLES_COUNT = HEADER_BYTES_COUNT * 8; // 304 samples at 1-bit LSB

    private final byte version;
    private final int bitsPerSample;
    private final byte[] salt;
    private final byte[] iv;
    private final int ciphertextLength;

    public StegoHeader(int bitsPerSample, byte[] salt, byte[] iv, int ciphertextLength) {
        this.version = VERSION;
        this.bitsPerSample = bitsPerSample;
        this.salt = salt;
        this.iv = iv;
        this.ciphertextLength = ciphertextLength;
    }

    public StegoHeader(byte version, int bitsPerSample, byte[] salt, byte[] iv, int ciphertextLength) {
        this.version = version;
        this.bitsPerSample = bitsPerSample;
        this.salt = salt;
        this.iv = iv;
        this.ciphertextLength = ciphertextLength;
    }

    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_BYTES_COUNT).order(ByteOrder.BIG_ENDIAN);
        buf.put(MAGIC);
        buf.put(version);
        buf.put((byte) bitsPerSample);
        buf.put(salt);
        buf.put(iv);
        buf.putInt(ciphertextLength);
        return buf.array();
    }

    public static StegoHeader deserialize(byte[] bytes) throws IllegalArgumentException {
        if (bytes.length < HEADER_BYTES_COUNT) {
            throw new IllegalArgumentException("Header data is too short: " + bytes.length + " bytes (expected " + HEADER_BYTES_COUNT + ")");
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        byte[] magic = new byte[4];
        buf.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new IllegalArgumentException("Audio file does not contain an EchoHide steganographic payload (Magic marker not found).");
        }

        byte ver = buf.get();
        int bitsPerSample = buf.get() & 0xFF;
        if (bitsPerSample < 1 || bitsPerSample > 4) {
            throw new IllegalArgumentException("Unsupported bitsPerSample in stego header: " + bitsPerSample);
        }

        byte[] salt = new byte[16];
        buf.get(salt);

        byte[] iv = new byte[12];
        buf.get(iv);

        int length = buf.getInt();
        if (length < 0 || length > 100_000_000) {
            throw new IllegalArgumentException("Invalid payload length in stego header: " + length);
        }

        return new StegoHeader(ver, bitsPerSample, salt, iv, length);
    }

    public byte getVersion() {
        return version;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getIv() {
        return iv;
    }

    public int getCiphertextLength() {
        return ciphertextLength;
    }
}

