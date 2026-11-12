package com.sayan.echohide.audio;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * Handles reading, writing, and sample manipulation for 16-bit PCM WAV audio files.
 * Features a resilient RIFF parser supporting standard PCM, WAVE_FORMAT_EXTENSIBLE,
 * 24-bit/32-bit PCM, and IEEE 32-bit float audio (with auto-normalization to 16-bit).
 */
public class WavAudio {

    private final AudioFormat audioFormat;
    private final short[] samples;
    private final int channels;
    private final float sampleRate;
    private final int totalFrames;

    public WavAudio(AudioFormat audioFormat, short[] samples) {
        this.audioFormat = audioFormat;
        this.samples = samples;
        this.channels = audioFormat.getChannels();
        this.sampleRate = audioFormat.getSampleRate();
        this.totalFrames = samples.length / Math.max(1, channels);
    }

    /**
     * Reads an audio file. Supports standard PCM WAV, Audacity/DAW exports (WAVE_FORMAT_EXTENSIBLE,
     * 24-bit, 32-bit float), and falls back to AudioSystem where needed.
     */
    public static WavAudio read(File file) throws IOException, UnsupportedAudioFileException {
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        if (fileBytes.length < 12) {
            throw new UnsupportedAudioFileException("File is too small to be a valid audio file (" + fileBytes.length + " bytes).");
        }

        // Check for common non-WAV formats and provide clear error messages
        if (fileBytes[0] == 'I' && fileBytes[1] == 'D' && fileBytes[2] == '3') {
            throw new UnsupportedAudioFileException("The file '" + file.getName() + "' is an MP3 file (ID3 header found), despite having a .wav extension.\n" +
                    "Audio steganography requires uncompressed PCM WAV audio because MP3 lossy compression erases LSB data.\n" +
                    "Please convert it to an uncompressed 16-bit PCM WAV file.");
        }
        if ((fileBytes[0] & 0xFF) == 0xFF && (fileBytes[1] & 0xE0) == 0xE0) {
            throw new UnsupportedAudioFileException("The file '" + file.getName() + "' is an MP3/MPEG audio file.\n" +
                    "Audio steganography requires uncompressed PCM WAV audio. Please convert it to a WAV file.");
        }
        if (fileBytes[0] == 'O' && fileBytes[1] == 'g' && fileBytes[2] == 'g' && fileBytes[3] == 'S') {
            throw new UnsupportedAudioFileException("The file '" + file.getName() + "' is an Ogg Vorbis file. Please convert it to 16-bit PCM WAV.");
        }
        if (fileBytes[0] == 'f' && fileBytes[1] == 'L' && fileBytes[2] == 'a' && fileBytes[3] == 'C') {
            throw new UnsupportedAudioFileException("The file '" + file.getName() + "' is a FLAC file. Please convert it to 16-bit PCM WAV.");
        }

        // RIFF WAVE Parsing
        String riffMagic = new String(fileBytes, 0, 4, StandardCharsets.US_ASCII);
        String waveMagic = new String(fileBytes, 8, 4, StandardCharsets.US_ASCII);

        if ("RIFF".equals(riffMagic) && "WAVE".equals(waveMagic)) {
            try {
                return parseRiffWave(fileBytes);
            } catch (Exception ex) {
                // Try fallback to AudioSystem
                try {
                    return readViaAudioSystem(file);
                } catch (Exception ignored) {
                    throw new UnsupportedAudioFileException("Could not parse WAV file: " + ex.getMessage());
                }
            }
        }

        // Fallback to standard Java Sound SPI
        return readViaAudioSystem(file);
    }

    private static WavAudio parseRiffWave(byte[] fileBytes) throws UnsupportedAudioFileException {
        int offset = 12;
        int fmtCode = -1;
        int channels = 0;
        int sampleRate = 0;
        int bitsPerSample = 0;
        byte[] dataBytes = null;

        while (offset + 8 <= fileBytes.length) {
            String chunkId = new String(fileBytes, offset, 4, StandardCharsets.US_ASCII);
            int chunkSize = ByteBuffer.wrap(fileBytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            offset += 8;

            if (chunkSize < 0 || offset + chunkSize > fileBytes.length) {
                chunkSize = Math.max(0, fileBytes.length - offset);
            }

            if ("fmt ".equals(chunkId) && chunkSize >= 16) {
                ByteBuffer fmtBuf = ByteBuffer.wrap(fileBytes, offset, chunkSize).order(ByteOrder.LITTLE_ENDIAN);
                fmtCode = fmtBuf.getShort() & 0xFFFF;
                channels = fmtBuf.getShort() & 0xFFFF;
                sampleRate = fmtBuf.getInt();
                int byteRate = fmtBuf.getInt();
                int blockAlign = fmtBuf.getShort() & 0xFFFF;
                bitsPerSample = fmtBuf.getShort() & 0xFFFF;

                // Handle WAVE_FORMAT_EXTENSIBLE (0xFFFE) common in Audacity/Windows
                if (fmtCode == 0xFFFE && chunkSize >= 40) {
                    fmtBuf.position(fmtBuf.position() + 8); // skip cbSize, validBits, channelMask
                    int subFmt = fmtBuf.getShort() & 0xFFFF; // SubFormat GUID starts with format tag
                    fmtCode = subFmt; // 1 = PCM, 3 = IEEE Float
                }
            } else if ("data".equals(chunkId)) {
                dataBytes = Arrays.copyOfRange(fileBytes, offset, offset + chunkSize);
            }

            offset += chunkSize;
            if ((chunkSize % 2) != 0) {
                offset++; // RIFF word alignment
            }
        }

        if (dataBytes == null || fmtCode == -1 || channels <= 0 || sampleRate <= 0) {
            throw new UnsupportedAudioFileException("Incomplete WAV header: fmt or data chunk missing.");
        }

        // fmtCode 1 = PCM, fmtCode 3 = IEEE Float
        if (fmtCode != 1 && fmtCode != 3) {
            throw new UnsupportedAudioFileException("Unsupported audio format code in WAV: " + fmtCode + 
                    " (Expected PCM or IEEE Float). Compressed WAV audio is not supported.");
        }

        short[] samples = convertTo16BitPcm(dataBytes, fmtCode, bitsPerSample, channels);
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        return new WavAudio(format, samples);
    }

    private static short[] convertTo16BitPcm(byte[] dataBytes, int fmtCode, int bitsPerSample, int channels)
            throws UnsupportedAudioFileException {
        ByteBuffer bb = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN);

        if (fmtCode == 1) { // PCM
            if (bitsPerSample == 16) {
                int count = dataBytes.length / 2;
                short[] samples = new short[count];
                for (int i = 0; i < count; i++) {
                    samples[i] = bb.getShort();
                }
                return samples;
            } else if (bitsPerSample == 24) {
                int count = dataBytes.length / 3;
                short[] samples = new short[count];
                for (int i = 0; i < count; i++) {
                    int b0 = bb.get() & 0xFF;
                    int b1 = bb.get() & 0xFF;
                    int b2 = bb.get(); // signed
                    int val24 = b0 | (b1 << 8) | (b2 << 16);
                    samples[i] = (short) (val24 >> 8); // Normalize to 16-bit
                }
                return samples;
            } else if (bitsPerSample == 32) {
                int count = dataBytes.length / 4;
                short[] samples = new short[count];
                for (int i = 0; i < count; i++) {
                    int val32 = bb.getInt();
                    samples[i] = (short) (val32 >> 16); // Normalize to 16-bit
                }
                return samples;
            } else if (bitsPerSample == 8) {
                int count = dataBytes.length;
                short[] samples = new short[count];
                for (int i = 0; i < count; i++) {
                    int unsignedByte = bb.get() & 0xFF;
                    samples[i] = (short) ((unsignedByte - 128) << 8); // 8-bit unsigned to 16-bit signed
                }
                return samples;
            }
        } else if (fmtCode == 3) { // IEEE 32-bit Float
            if (bitsPerSample == 32) {
                int count = dataBytes.length / 4;
                short[] samples = new short[count];
                for (int i = 0; i < count; i++) {
                    float f = bb.getFloat();
                    float clamped = Math.max(-1.0f, Math.min(1.0f, f));
                    samples[i] = (short) Math.round(clamped * 32767.0f);
                }
                return samples;
            }
        }

        throw new UnsupportedAudioFileException("Unsupported sample bit-depth: " + bitsPerSample + "-bit format code " + fmtCode);
    }

    private static WavAudio readViaAudioSystem(File file) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
            AudioFormat format = ais.getFormat();

            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                throw new UnsupportedAudioFileException("Only PCM_SIGNED WAV audio is supported. Found: " + format.getEncoding());
            }

            if (format.getSampleSizeInBits() != 16) {
                throw new UnsupportedAudioFileException("Only 16-bit PCM WAV audio is supported. Found: " + format.getSampleSizeInBits() + "-bit");
            }

            byte[] audioBytes = ais.readAllBytes();
            int numSamples = audioBytes.length / 2;
            short[] samples = new short[numSamples];

            boolean bigEndian = format.isBigEndian();
            ByteBuffer bb = ByteBuffer.wrap(audioBytes).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < numSamples; i++) {
                samples[i] = bb.getShort();
            }

            return new WavAudio(format, samples);
        }
    }

    /**
     * Writes the current audio samples into a standard 16-bit PCM WAV file.
     */
    public void save(File outputFile) throws IOException {
        int numSamples = samples.length;
        byte[] audioBytes = new byte[numSamples * 2];
        boolean bigEndian = audioFormat.isBigEndian();
        ByteBuffer bb = ByteBuffer.wrap(audioBytes).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        for (short sample : samples) {
            bb.putShort(sample);
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
             AudioInputStream ais = new AudioInputStream(bais, audioFormat, (long) numSamples / audioFormat.getChannels())) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
        }
    }

    public WavAudio withSamples(short[] newSamples) {
        return new WavAudio(this.audioFormat, newSamples);
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public short[] getSamples() {
        return samples;
    }

    public int getChannels() {
        return channels;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getTotalSamples() {
        return samples.length;
    }

    public double getDurationSeconds() {
        if (sampleRate <= 0 || channels <= 0) return 0;
        return (double) samples.length / (sampleRate * channels);
    }

    public int getSample(int index) {
        return samples[index];
    }

    public void setSample(int index, short value) {
        samples[index] = value;
    }

    public WavAudio copy() {
        short[] copy = Arrays.copyOf(samples, samples.length);
        return new WavAudio(this.audioFormat, copy);
    }
}
