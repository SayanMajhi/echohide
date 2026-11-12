package com.sayan.echohide.metrics;

import com.sayan.echohide.audio.WavAudio;
import com.sayan.echohide.stego.LsbSteganography;

/**
 * Calculates audio quality metrics (SNR, PSNR, MSE) and steganography performance benchmarks.
 */
public class AudioMetrics {

    public static class MetricsResult {
        private final double snrDb;
        private final double psnrDb;
        private final double mse;
        private final int modifiedSamplesCount;
        private final double modifiedPercentage;
        private final int totalSamples;
        private final long embeddingTimeMs;
        private final long extractionTimeMs;
        private final boolean recoverySuccess;

        public MetricsResult(double snrDb, double psnrDb, double mse, int modifiedSamplesCount,
                             double modifiedPercentage, int totalSamples, long embeddingTimeMs,
                             long extractionTimeMs, boolean recoverySuccess) {
            this.snrDb = snrDb;
            this.psnrDb = psnrDb;
            this.mse = mse;
            this.modifiedSamplesCount = modifiedSamplesCount;
            this.modifiedPercentage = modifiedPercentage;
            this.totalSamples = totalSamples;
            this.embeddingTimeMs = embeddingTimeMs;
            this.extractionTimeMs = extractionTimeMs;
            this.recoverySuccess = recoverySuccess;
        }

        public double getSnrDb() {
            return snrDb;
        }

        public double getPsnrDb() {
            return psnrDb;
        }

        public double getMse() {
            return mse;
        }

        public int getModifiedSamplesCount() {
            return modifiedSamplesCount;
        }

        public double getModifiedPercentage() {
            return modifiedPercentage;
        }

        public int getTotalSamples() {
            return totalSamples;
        }

        public long getEmbeddingTimeMs() {
            return embeddingTimeMs;
        }

        public long getExtractionTimeMs() {
            return extractionTimeMs;
        }

        public boolean isRecoverySuccess() {
            return recoverySuccess;
        }

        @Override
        public String toString() {
            return String.format("SNR: %.2f dB | PSNR: %.2f dB | MSE: %.4f | Modified: %d / %d (%.2f%%) | Recovery: %s",
                    snrDb, psnrDb, mse, modifiedSamplesCount, totalSamples, modifiedPercentage,
                    recoverySuccess ? "PASSED (100% Match)" : "FAILED");
        }
    }

    /**
     * Calculates SNR, PSNR, and MSE between carrier and stego audio files.
     */
    public static MetricsResult evaluate(WavAudio original, WavAudio stego, long embedTimeMs, long extractTimeMs, boolean recoverySuccess) {
        short[] origSamples = original.getSamples();
        short[] stegoSamples = stego.getSamples();

        int n = Math.min(origSamples.length, stegoSamples.length);
        if (n == 0) {
            return new MetricsResult(0, 0, 0, 0, 0, 0, embedTimeMs, extractTimeMs, recoverySuccess);
        }

        double sumSignalSq = 0.0;
        double sumNoiseSq = 0.0;
        int modifiedCount = 0;

        for (int i = 0; i < n; i++) {
            double origVal = origSamples[i];
            double stegoVal = stegoSamples[i];
            double diff = origVal - stegoVal;

            sumSignalSq += origVal * origVal;
            sumNoiseSq += diff * diff;

            if (diff != 0) {
                modifiedCount++;
            }
        }

        double mse = sumNoiseSq / n;

        double snrDb;
        if (sumNoiseSq == 0.0) {
            snrDb = 120.0; // Perfect match / mathematically infinite SNR, cap at 120 dB
        } else if (sumSignalSq == 0.0) {
            snrDb = 0.0;
        } else {
            snrDb = 10.0 * Math.log10(sumSignalSq / sumNoiseSq);
        }

        // 16-bit audio max amplitude is 32767
        double maxSample = 32767.0;
        double psnrDb;
        if (mse == 0.0) {
            psnrDb = 120.0;
        } else {
            psnrDb = 10.0 * Math.log10((maxSample * maxSample) / mse);
        }

        double modPercent = ((double) modifiedCount / n) * 100.0;

        return new MetricsResult(snrDb, psnrDb, mse, modifiedCount, modPercent, n, embedTimeMs, extractTimeMs, recoverySuccess);
    }
}

