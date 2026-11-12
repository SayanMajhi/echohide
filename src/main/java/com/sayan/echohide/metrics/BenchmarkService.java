package com.sayan.echohide.metrics;

import com.sayan.echohide.audio.AudioToneGenerator;
import com.sayan.echohide.audio.WavAudio;
import com.sayan.echohide.stego.LsbSteganography;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs systematic benchmarks across different payload sizes and LSB depths.
 */
public class BenchmarkService {

    public static class BenchmarkRow {
        public final int payloadBytes;
        public final int bitsPerSample;
        public final double snrDb;
        public final double psnrDb;
        public final double mse;
        public final long embedTimeMs;
        public final long extractTimeMs;
        public final boolean recoverySuccess;
        public final double carrierUsagePercent;

        public BenchmarkRow(int payloadBytes, int bitsPerSample, double snrDb, double psnrDb, double mse,
                            long embedTimeMs, long extractTimeMs, boolean recoverySuccess, double carrierUsagePercent) {
            this.payloadBytes = payloadBytes;
            this.bitsPerSample = bitsPerSample;
            this.snrDb = snrDb;
            this.psnrDb = psnrDb;
            this.mse = mse;
            this.embedTimeMs = embedTimeMs;
            this.extractTimeMs = extractTimeMs;
            this.recoverySuccess = recoverySuccess;
            this.carrierUsagePercent = carrierUsagePercent;
        }
    }

    /**
     * Executes benchmark runs on the given carrier audio (or generates a standard 5s test audio if null).
     */
    public static List<BenchmarkRow> runBenchmark(WavAudio carrier) throws Exception {
        if (carrier == null) {
            carrier = AudioToneGenerator.generateChime(5.0, 44100, true, null);
        }

        int[] payloadSizes = { 32, 128, 512, 2048, 8192 };
        int[] lsbOptions = { 1, 2 };
        String password = "BenchmarkPassphrase#2026";

        List<BenchmarkRow> rows = new ArrayList<>();

        for (int bits : lsbOptions) {
            for (int size : payloadSizes) {
                // Ensure carrier has capacity
                int maxCap = LsbSteganography.calculateMaxPayloadBytes(carrier.getTotalSamples(), bits);
                if (size > maxCap) {
                    continue;
                }

                String payload = generateTestPayload(size);

                long t0 = System.nanoTime();
                LsbSteganography.EmbeddingResult embedRes = LsbSteganography.embed(carrier, payload, password, bits);
                long t1 = System.nanoTime();
                long embedTimeMs = (t1 - t0) / 1_000_000;

                long t2 = System.nanoTime();
                String recovered = LsbSteganography.extract(embedRes.getStegoAudio(), password);
                long t3 = System.nanoTime();
                long extractTimeMs = (t3 - t2) / 1_000_000;

                boolean success = payload.equals(recovered);

                AudioMetrics.MetricsResult metrics = AudioMetrics.evaluate(carrier, embedRes.getStegoAudio(),
                        embedTimeMs, extractTimeMs, success);

                rows.add(new BenchmarkRow(
                        size,
                        bits,
                        metrics.getSnrDb(),
                        metrics.getPsnrDb(),
                        metrics.getMse(),
                        embedTimeMs,
                        extractTimeMs,
                        success,
                        embedRes.getCapacityUsagePercentage()
                ));
            }
        }

        return rows;
    }

    private static String generateTestPayload(int byteCount) {
        StringBuilder sb = new StringBuilder(byteCount);
        String sampleText = "The quick brown fox jumps over the lazy dog. EchoHide Audio Steganography Project 2026. ";
        while (sb.length() < byteCount) {
            sb.append(sampleText);
        }
        return sb.substring(0, byteCount);
    }
}

