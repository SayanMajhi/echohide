package com.sayan.echohide.cli;

import com.sayan.echohide.audio.AudioToneGenerator;
import com.sayan.echohide.audio.WavAudio;
import com.sayan.echohide.crypto.AuthenticationFailedException;
import com.sayan.echohide.metrics.AudioMetrics;
import com.sayan.echohide.metrics.BenchmarkService;
import com.sayan.echohide.network.TcpReceiver;
import com.sayan.echohide.network.TcpSender;
import com.sayan.echohide.network.TcpTransferListener;
import com.sayan.echohide.stego.LsbSteganography;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Handles command-line execution and automated testing modes.
 */
public class CliController {

    public static void printHelp() {
        System.out.println("===============================================================================");
        System.out.println("  EchoHide: Lossless Audio Steganography with AES-GCM and TCP Transfer");
        System.out.println("===============================================================================");
        System.out.println("Usage:");
        System.out.println("  java -jar echohide.jar [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  --gui                                 Launch interactive Swing GUI (default if no args)");
        System.out.println("  --generate-sample -out <file> [-sec <s=5.0>] Generate test 16-bit PCM WAV chord");
        System.out.println("  --embed -in <wav> -msg <text> -pass <pwd> -out <stego.wav> [-bits <1|2|4>]");
        System.out.println("  --extract -in <stego.wav> -pass <pwd>");
        System.out.println("  --metrics -orig <wav> -stego <stego.wav>");
        System.out.println("  --benchmark [-carrier <wav>]          Run systematic capacity/SNR/time benchmarks");
        System.out.println("  --send -host <host> -port <p> -file <stego.wav>  Send file via framed TCP socket");
        System.out.println("  --recv -port <p> -dir <outDir>        Receive file via framed TCP socket");
        System.out.println("  --help, -h                            Show this help menu");
        System.out.println("===============================================================================");
    }

    public static void run(String[] args) throws Exception {
        if (args.length == 0 || args[0].equalsIgnoreCase("--gui")) {
            // Handled in Main
            return;
        }

        String cmd = args[0].toLowerCase();
        switch (cmd) {
            case "--help":
            case "-h":
                printHelp();
                break;

            case "--generate-sample":
                handleGenerateSample(args);
                break;

            case "--embed":
                handleEmbed(args);
                break;

            case "--extract":
                handleExtract(args);
                break;

            case "--metrics":
                handleMetrics(args);
                break;

            case "--benchmark":
                handleBenchmark(args);
                break;

            case "--send":
                handleSend(args);
                break;

            case "--recv":
                handleRecv(args);
                break;

            default:
                System.err.println("Unknown command: " + cmd);
                printHelp();
                System.exit(1);
        }
    }

    private static void handleGenerateSample(String[] args) throws Exception {
        String outPath = getArg(args, "-out", "sample_carrier.wav");
        double duration = Double.parseDouble(getArg(args, "-sec", "5.0"));
        File outFile = new File(outPath);
        System.out.println("[EchoHide] Generating synthetic 16-bit PCM stereo WAV tone (" + duration + "s)...");
        WavAudio audio = AudioToneGenerator.generateChime(duration, 44100, true, outFile);
        System.out.println("[EchoHide] Sample saved: " + outFile.getAbsolutePath());
        System.out.println(String.format("           Total Samples: %,d | Sample Rate: %.0f Hz | Duration: %.2fs",
                audio.getTotalSamples(), audio.getSampleRate(), audio.getDurationSeconds()));
    }

    private static void handleEmbed(String[] args) throws Exception {
        String inPath = getRequiredArg(args, "-in");
        String message = getRequiredArg(args, "-msg");
        String password = getRequiredArg(args, "-pass");
        String outPath = getRequiredArg(args, "-out");
        int bits = Integer.parseInt(getArg(args, "-bits", "1"));

        File inFile = new File(inPath);
        File outFile = new File(outPath);

        System.out.println("[EchoHide] Loading carrier: " + inFile.getName());
        WavAudio carrier = WavAudio.read(inFile);

        long t0 = System.nanoTime();
        LsbSteganography.EmbeddingResult result = LsbSteganography.embed(carrier, message, password, bits);
        long t1 = System.nanoTime();
        long embedTimeMs = (t1 - t0) / 1_000_000;

        result.getStegoAudio().save(outFile);

        AudioMetrics.MetricsResult metrics = AudioMetrics.evaluate(carrier, result.getStegoAudio(), embedTimeMs, 0, true);

        System.out.println("[EchoHide] Stego audio successfully generated and saved to: " + outFile.getName());
        System.out.println(String.format("           Payload size: %d bytes | Bits per sample: %d LSB", message.getBytes().length, bits));
        System.out.println(String.format("           Carrier capacity used: %.2f%% (%d / %d samples)",
                result.getCapacityUsagePercentage(), result.getSamplesUsed(), result.getTotalCarrierSamples()));
        System.out.println(String.format("           SNR: %.2f dB | PSNR: %.2f dB | MSE: %.4f",
                metrics.getSnrDb(), metrics.getPsnrDb(), metrics.getMse()));
        System.out.println("           Embedding time: " + embedTimeMs + " ms");
    }

    private static void handleExtract(String[] args) throws Exception {
        String inPath = getRequiredArg(args, "-in");
        String password = getRequiredArg(args, "-pass");
        File inFile = new File(inPath);

        System.out.println("[EchoHide] Reading stego audio: " + inFile.getName());
        WavAudio stego = WavAudio.read(inFile);

        long t0 = System.nanoTime();
        try {
            String recovered = LsbSteganography.extract(stego, password);
            long t1 = System.nanoTime();
            long extractTimeMs = (t1 - t0) / 1_000_000;

            System.out.println("[EchoHide] SUCCESS! Authenticated decryption passed.");
            System.out.println("           Extraction time: " + extractTimeMs + " ms");
            System.out.println("------------------ RECOVERED SECRET MESSAGE ------------------");
            System.out.println(recovered);
            System.out.println("--------------------------------------------------------------");
        } catch (AuthenticationFailedException afe) {
            System.err.println("[EchoHide] FAILED: " + afe.getMessage());
            System.exit(2);
        }
    }

    private static void handleMetrics(String[] args) throws Exception {
        String origPath = getRequiredArg(args, "-orig");
        String stegoPath = getRequiredArg(args, "-stego");

        WavAudio orig = WavAudio.read(new File(origPath));
        WavAudio stego = WavAudio.read(new File(stegoPath));

        AudioMetrics.MetricsResult m = AudioMetrics.evaluate(orig, stego, 0, 0, true);
        System.out.println("================== AUDIO STEGANOGRAPHY METRICS ==================");
        System.out.println(String.format("Carrier Samples  : %,d", m.getTotalSamples()));
        System.out.println(String.format("Modified Samples : %,d (%.2f%%)", m.getModifiedSamplesCount(), m.getModifiedPercentage()));
        System.out.println(String.format("MSE (Mean Sq Err): %.6f", m.getMse()));
        System.out.println(String.format("SNR              : %.2f dB", m.getSnrDb()));
        System.out.println(String.format("PSNR             : %.2f dB", m.getPsnrDb()));
        System.out.println("=================================================================");
    }

    private static void handleBenchmark(String[] args) throws Exception {
        String carrierPath = getArg(args, "-carrier", null);
        WavAudio carrier = null;
        if (carrierPath != null) {
            carrier = WavAudio.read(new File(carrierPath));
            System.out.println("[EchoHide] Running benchmarks on carrier: " + carrierPath);
        } else {
            System.out.println("[EchoHide] Generating 5s test carrier for systematic benchmark...");
        }

        List<BenchmarkService.BenchmarkRow> rows = BenchmarkService.runBenchmark(carrier);

        System.out.println();
        System.out.println("=========================================================================================================");
        System.out.println("                                  ECHOHIDE PERFORMANCE BENCHMARK RESULTS                                 ");
        System.out.println("=========================================================================================================");
        System.out.printf("%-12s | %-6s | %-10s | %-10s | %-10s | %-10s | %-10s | %-12s%n",
                "Payload (B)", "LSB", "SNR (dB)", "PSNR (dB)", "MSE", "Embed(ms)", "Extr(ms)", "Recovery");
        System.out.println("---------------------------------------------------------------------------------------------------------");
        for (BenchmarkService.BenchmarkRow r : rows) {
            System.out.printf("%-12d | %-6d | %-10.2f | %-10.2f | %-10.4f | %-10d | %-10d | %-12s%n",
                    r.payloadBytes, r.bitsPerSample, r.snrDb, r.psnrDb, r.mse, r.embedTimeMs, r.extractTimeMs,
                    r.recoverySuccess ? "SUCCESS" : "FAIL");
        }
        System.out.println("=========================================================================================================");
    }

    private static void handleSend(String[] args) throws Exception {
        String host = getArg(args, "-host", "localhost");
        int port = Integer.parseInt(getArg(args, "-port", "9876"));
        String filePath = getRequiredArg(args, "-file");
        File file = new File(filePath);

        CountDownLatch latch = new CountDownLatch(1);
        System.out.println("[EchoHide TCP Sender] Sending " + file.getName() + " to " + host + ":" + port + "...");

        TcpSender.sendFile(host, port, file, new TcpTransferListener() {
            @Override
            public void onProgress(long bytesTransferred, long totalBytes, String statusMessage) {
                System.out.println("  -> " + statusMessage);
            }

            @Override
            public void onComplete(String successMessage) {
                System.out.println("[EchoHide TCP Sender] " + successMessage);
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                System.err.println("[EchoHide TCP Sender ERROR] " + exception.getMessage());
                latch.countDown();
            }
        });

        latch.await();
    }

    private static void handleRecv(String[] args) throws Exception {
        int port = Integer.parseInt(getArg(args, "-port", "9876"));
        String dirPath = getArg(args, "-dir", "received_audio");
        File dir = new File(dirPath);

        System.out.println("[EchoHide TCP Receiver] Starting listener on port " + port + ", saving to: " + dir.getAbsolutePath());
        CountDownLatch latch = new CountDownLatch(1);

        TcpReceiver receiver = new TcpReceiver(port, dir, new TcpTransferListener() {
            @Override
            public void onProgress(long bytesTransferred, long totalBytes, String statusMessage) {
                System.out.println("  <- " + statusMessage);
            }

            @Override
            public void onComplete(String successMessage) {
                System.out.println("[EchoHide TCP Receiver] " + successMessage);
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                System.err.println("[EchoHide TCP Receiver ERROR] " + exception.getMessage());
            }
        });

        receiver.start();
        System.out.println("[EchoHide TCP Receiver] Waiting for 1 file transfer (press Ctrl+C to stop)...");
        latch.await();
        receiver.stop();
        System.out.println("[EchoHide TCP Receiver] Transfer complete. Receiver stopped.");
    }

    private static String getArg(String[] args, String flag, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(flag)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    private static String getRequiredArg(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(flag)) {
                return args[i + 1];
            }
        }
        System.err.println("Error: Missing required argument " + flag);
        printHelp();
        System.exit(1);
        return null;
    }
}

