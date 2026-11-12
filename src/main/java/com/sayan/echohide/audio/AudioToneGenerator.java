package com.sayan.echohide.audio;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;

/**
 * Utility to generate synthetic 16-bit PCM WAV audio files for testing and evaluation.
 */
public class AudioToneGenerator {

    /**
     * Generates a 16-bit PCM harmonic chord WAV file (e.g. 440Hz A4 + 554Hz C#5 + 659Hz E5)
     * with smooth ADSR envelope to avoid clicks.
     *
     * @param durationSeconds Duration of audio in seconds
     * @param sampleRate e.g. 44100
     * @param stereo whether 2 channels or mono
     * @param outputFile target file
     */
    public static WavAudio generateChime(double durationSeconds, float sampleRate, boolean stereo, File outputFile) throws IOException {
        int channels = stereo ? 2 : 1;
        int totalFrames = (int) (durationSeconds * sampleRate);
        int totalSamples = totalFrames * channels;
        short[] samples = new short[totalSamples];

        double[] frequencies = { 440.0, 554.37, 659.25, 880.0 }; // A major triad with octave
        double[] weights = { 0.45, 0.25, 0.20, 0.10 };

        for (int frame = 0; frame < totalFrames; frame++) {
            double t = (double) frame / sampleRate;

            // Attack-Decay-Sustain-Release Envelope
            double envelope = 1.0;
            if (t < 0.05) {
                envelope = t / 0.05; // 50ms attack
            } else if (t > durationSeconds - 0.2) {
                envelope = Math.max(0.0, (durationSeconds - t) / 0.2); // 200ms release
            } else {
                // gentle exponential decay
                envelope = Math.exp(-0.4 * t);
            }

            double sampleVal = 0.0;
            for (int f = 0; f < frequencies.length; f++) {
                sampleVal += weights[f] * Math.sin(2.0 * Math.PI * frequencies[f] * t);
            }

            // Scale to 16-bit signed short range (target peak amplitude ~ 20000)
            short amp = (short) Math.round(sampleVal * envelope * 22000.0);

            if (stereo) {
                samples[frame * 2] = amp;
                // slight phase/panning for right channel
                samples[frame * 2 + 1] = (short) Math.round(amp * 0.95);
            } else {
                samples[frame] = amp;
            }
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        WavAudio audio = new WavAudio(format, samples);
        if (outputFile != null) {
            audio.save(outputFile);
        }
        return audio;
    }

    /**
     * Generates a 440 Hz standard pure tone WAV file.
     */
    public static WavAudio generateSineWave(double durationSeconds, float sampleRate, double frequency, File outputFile) throws IOException {
        int channels = 1;
        int totalFrames = (int) (durationSeconds * sampleRate);
        short[] samples = new short[totalFrames];

        for (int i = 0; i < totalFrames; i++) {
            double t = (double) i / sampleRate;
            double val = Math.sin(2.0 * Math.PI * frequency * t);
            samples[i] = (short) Math.round(val * 24000.0);
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        WavAudio audio = new WavAudio(format, samples);
        if (outputFile != null) {
            audio.save(outputFile);
        }
        return audio;
    }
}

