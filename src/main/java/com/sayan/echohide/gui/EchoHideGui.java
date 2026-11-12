package com.sayan.echohide.gui;

import com.sayan.echohide.audio.AudioPlayer;
import com.sayan.echohide.audio.AudioToneGenerator;
import com.sayan.echohide.audio.WavAudio;
import com.sayan.echohide.crypto.AuthenticationFailedException;
import com.sayan.echohide.metrics.AudioMetrics;
import com.sayan.echohide.metrics.BenchmarkService;
import com.sayan.echohide.network.TcpReceiver;
import com.sayan.echohide.network.TcpSender;
import com.sayan.echohide.network.TcpTransferListener;
import com.sayan.echohide.stego.LsbSteganography;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Modern Swing Graphical User Interface for EchoHide.
 */
public class EchoHideGui extends JFrame {

    private final AudioPlayer audioPlayer = new AudioPlayer();

    // Tab 1: Embed components
    private JTextField carrierPathField;
    private JTextArea secretMessageArea;
    private JPasswordField embedPasswordField;
    private JComboBox<String> lsbDepthCombo;
    private JTextField stegoOutputPathField;
    private JLabel embedStatusLabel;
    private JLabel embedCapacityLabel;
    private JLabel embedSnrLabel;
    private File currentCarrierFile;
    private File currentStegoOutputFile;

    // Tab 2: Extract components
    private JTextField extractStegoPathField;
    private JPasswordField extractPasswordField;
    private JTextArea recoveredMessageArea;
    private JLabel extractStatusLabel;
    private File currentExtractStegoFile;

    // Tab 3: TCP Transfer components
    private JTextField serverPortField;
    private JTextField serverDirField;
    private JButton startServerBtn;
    private JButton stopServerBtn;
    private JLabel serverStatusLabel;
    private TcpReceiver activeReceiver;

    private JTextField clientHostField;
    private JTextField clientPortField;
    private JTextField clientFileField;
    private JProgressBar transferProgressBar;
    private JTextArea transferLogArea;

    // Tab 4: Evaluation Metrics & Benchmark components
    private JTextField metricsOrigField;
    private JTextField metricsStegoField;
    private JLabel metricsSnrVal;
    private JLabel metricsPsnrVal;
    private JLabel metricsMseVal;
    private JLabel metricsModifiedVal;
    private JTable benchmarkTable;
    private DefaultTableModel benchmarkTableModel;

    public EchoHideGui() {
        setTitle("EchoHide - Lossless Audio Steganography & Encrypted TCP Transfer");
        setSize(950, 720);
        setMinimumSize(new Dimension(850, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUi();
    }

    private void initUi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 43, 73));
        headerPanel.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel titleLabel = new JLabel("EchoHide: 16-bit PCM Audio Steganography");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Authenticated AES-256-GCM Encryption • LSB Embedding • TCP Socket Transfer • Audio SNR Evaluation");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(180, 205, 235));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        root.add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));

        tabbedPane.addTab("  1. Embed Secret Message  ", createEmbedTab());
        tabbedPane.addTab("  2. Extract & Decrypt  ", createExtractTab());
        tabbedPane.addTab("  3. TCP Network Transfer  ", createTcpTransferTab());
        tabbedPane.addTab("  4. Evaluation & Benchmarks  ", createMetricsTab());

        root.add(tabbedPane, BorderLayout.CENTER);

        // Bottom playback control bar
        JPanel playbackBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JLabel playbackInfo = new JLabel("Audio Playback Controls:");
        playbackInfo.setFont(new Font("SansSerif", Font.ITALIC, 12));
        JButton stopAudioBtn = new JButton("⏹ Stop Playing Audio");
        stopAudioBtn.addActionListener(e -> {
            audioPlayer.stop();
            JOptionPane.showMessageDialog(this, "Audio playback stopped.", "Playback", JOptionPane.INFORMATION_MESSAGE);
        });
        playbackBar.add(playbackInfo);
        playbackBar.add(stopAudioBtn);
        root.add(playbackBar, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // =========================================================================
    // TAB 1: EMBED
    // =========================================================================
    private JPanel createEmbedTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Carrier Audio Selection
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Carrier WAV File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        carrierPathField = new JTextField();
        formPanel.add(carrierPathField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JButton browseCarrierBtn = new JButton("Browse WAV...");
        browseCarrierBtn.addActionListener(e -> chooseCarrierFile());
        formPanel.add(browseCarrierBtn, gbc);

        gbc.gridx = 3;
        JButton generateSampleBtn = new JButton("Generate Test WAV");
        generateSampleBtn.setToolTipText("Synthesizes a 16-bit PCM harmonic chord test audio file");
        generateSampleBtn.addActionListener(e -> generateTestCarrier());
        formPanel.add(generateSampleBtn, gbc);

        // Secret Message
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(new JLabel("Secret Message:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        secretMessageArea = new JTextArea(4, 30);
        secretMessageArea.setLineWrap(true);
        secretMessageArea.setWrapStyleWord(true);
        secretMessageArea.setText("CONFIDENTIAL INTEL: System deployment scheduled at 18:00 UTC. Authorized node verification enabled.");
        JScrollPane msgScroll = new JScrollPane(secretMessageArea);
        formPanel.add(msgScroll, gbc);

        // Password & LSB Depth
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Encryption Password:"), gbc);

        gbc.gridx = 1;
        embedPasswordField = new JPasswordField("EchoHideSecKey#2026");
        formPanel.add(embedPasswordField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("LSB Depth:"), gbc);

        gbc.gridx = 3;
        lsbDepthCombo = new JComboBox<>(new String[]{"1 LSB (Highest Quality / Best SNR)", "2 LSB (Balanced / 2x Capacity)", "4 LSB (High Capacity)"});
        formPanel.add(lsbDepthCombo, gbc);

        // Output File
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Output Stego WAV:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        stegoOutputPathField = new JTextField("stego_audio.wav");
        formPanel.add(stegoOutputPathField, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        JButton browseOutBtn = new JButton("Select Output...");
        browseOutBtn.addActionListener(e -> chooseOutputFile());
        formPanel.add(browseOutBtn, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Action and Status Panel
        JPanel actionPanel = new JPanel(new BorderLayout(8, 8));
        actionPanel.setBorder(new TitledBorder("Embedding Control & Quality Feedback"));

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JButton embedBtn = new JButton("🔒 Embed Message & Save Stego WAV");
        embedBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        embedBtn.setBackground(new Color(33, 115, 70));
        embedBtn.setForeground(Color.WHITE);
        embedBtn.addActionListener(e -> performEmbedding());

        JButton playCarrierBtn = new JButton("▶ Play Original Audio");
        playCarrierBtn.addActionListener(e -> playAudioFile(currentCarrierFile));

        JButton playStegoBtn = new JButton("▶ Play Stego Audio");
        playStegoBtn.addActionListener(e -> playAudioFile(currentStegoOutputFile));

        buttonsRow.add(embedBtn);
        buttonsRow.add(playCarrierBtn);
        buttonsRow.add(playStegoBtn);

        JPanel statusCol = new JPanel(new GridLayout(3, 1, 4, 4));
        statusCol.setBorder(new EmptyBorder(4, 8, 4, 8));
        embedStatusLabel = new JLabel("Status: Ready. Select a carrier WAV and enter your message.");
        embedCapacityLabel = new JLabel("Capacity: Not loaded");
        embedSnrLabel = new JLabel("SNR / PSNR: Not calculated yet");

        statusCol.add(embedStatusLabel);
        statusCol.add(embedCapacityLabel);
        statusCol.add(embedSnrLabel);

        actionPanel.add(buttonsRow, BorderLayout.NORTH);
        actionPanel.add(statusCol, BorderLayout.CENTER);

        panel.add(actionPanel, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // TAB 2: EXTRACT
    // =========================================================================
    private JPanel createExtractTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Stego WAV Selection
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Stego WAV File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        extractStegoPathField = new JTextField("stego_audio.wav");
        formPanel.add(extractStegoPathField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JButton browseStegoBtn = new JButton("Browse Stego WAV...");
        browseStegoBtn.addActionListener(e -> chooseExtractStegoFile());
        formPanel.add(browseStegoBtn, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Decryption Password:"), gbc);

        gbc.gridx = 1;
        extractPasswordField = new JPasswordField("EchoHideSecKey#2026");
        formPanel.add(extractPasswordField, gbc);

        gbc.gridx = 2;
        JButton extractBtn = new JButton("🔓 Extract & Decrypt");
        extractBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        extractBtn.setBackground(new Color(30, 80, 160));
        extractBtn.setForeground(Color.WHITE);
        extractBtn.addActionListener(e -> performExtraction());
        formPanel.add(extractBtn, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        // Output Display
        JPanel displayPanel = new JPanel(new BorderLayout(6, 6));
        displayPanel.setBorder(new TitledBorder("Recovered Secret Text (AES-GCM Authenticated)"));

        recoveredMessageArea = new JTextArea();
        recoveredMessageArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        recoveredMessageArea.setEditable(false);
        recoveredMessageArea.setLineWrap(true);
        recoveredMessageArea.setWrapStyleWord(true);
        displayPanel.add(new JScrollPane(recoveredMessageArea), BorderLayout.CENTER);

        JPanel bottomStatus = new JPanel(new BorderLayout());
        extractStatusLabel = new JLabel("Status: Awaiting stego file and extraction trigger.");
        extractStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        JButton playExtractAudioBtn = new JButton("▶ Listen to Stego Audio");
        playExtractAudioBtn.addActionListener(e -> {
            File f = currentExtractStegoFile != null ? currentExtractStegoFile : new File(extractStegoPathField.getText().trim());
            playAudioFile(f);
        });

        bottomStatus.add(extractStatusLabel, BorderLayout.CENTER);
        bottomStatus.add(playExtractAudioBtn, BorderLayout.EAST);
        displayPanel.add(bottomStatus, BorderLayout.SOUTH);

        panel.add(displayPanel, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // TAB 3: TCP TRANSFER
    // =========================================================================
    private JPanel createTcpTransferTab() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Receiver Server Box
        JPanel recvPanel = new JPanel(new GridBagLayout());
        recvPanel.setBorder(new TitledBorder("TCP Receiver (Server Mode: Listen for Incoming Stego WAV)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        recvPanel.add(new JLabel("Listen Port:"), gbc);
        gbc.gridx = 1;
        serverPortField = new JTextField("9876", 6);
        recvPanel.add(serverPortField, gbc);

        gbc.gridx = 2;
        recvPanel.add(new JLabel("Save Directory:"), gbc);
        gbc.gridx = 3; gbc.weightx = 1.0;
        serverDirField = new JTextField("received_audio");
        recvPanel.add(serverDirField, gbc);

        gbc.gridx = 4; gbc.weightx = 0.0;
        startServerBtn = new JButton("Start Server");
        startServerBtn.addActionListener(e -> startTcpServer());
        recvPanel.add(startServerBtn, gbc);

        gbc.gridx = 5;
        stopServerBtn = new JButton("Stop Server");
        stopServerBtn.setEnabled(false);
        stopServerBtn.addActionListener(e -> stopTcpServer());
        recvPanel.add(stopServerBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 6;
        serverStatusLabel = new JLabel("Server Status: Idle / Stopped");
        serverStatusLabel.setForeground(Color.GRAY);
        recvPanel.add(serverStatusLabel, gbc);

        // Sender Client Box
        JPanel sendPanel = new JPanel(new BorderLayout(6, 6));
        sendPanel.setBorder(new TitledBorder("TCP Sender (Client Mode: Length-Framed File Transfer)"));

        JPanel sendForm = new JPanel(new GridBagLayout());
        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(5, 5, 5, 5);
        g2.fill = GridBagConstraints.HORIZONTAL;

        g2.gridx = 0; g2.gridy = 0;
        sendForm.add(new JLabel("Target Host:"), g2);
        g2.gridx = 1;
        clientHostField = new JTextField("localhost", 10);
        sendForm.add(clientHostField, g2);

        g2.gridx = 2;
        sendForm.add(new JLabel("Port:"), g2);
        g2.gridx = 3;
        clientPortField = new JTextField("9876", 6);
        sendForm.add(clientPortField, g2);

        g2.gridx = 4;
        sendForm.add(new JLabel("File to Send:"), g2);
        g2.gridx = 5; g2.weightx = 1.0;
        clientFileField = new JTextField("stego_audio.wav");
        sendForm.add(clientFileField, g2);

        g2.gridx = 6; g2.weightx = 0.0;
        JButton browseSendFileBtn = new JButton("Browse...");
        browseSendFileBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(new File("."));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                clientFileField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        sendForm.add(browseSendFileBtn, g2);

        g2.gridx = 7;
        JButton sendBtn = new JButton("📤 Send Over TCP");
        sendBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        sendBtn.setBackground(new Color(30, 110, 150));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.addActionListener(e -> sendFileOverTcp());
        sendForm.add(sendBtn, g2);

        sendPanel.add(sendForm, BorderLayout.NORTH);

        transferProgressBar = new JProgressBar(0, 100);
        transferProgressBar.setStringPainted(true);
        sendPanel.add(transferProgressBar, BorderLayout.CENTER);

        transferLogArea = new JTextArea(4, 40);
        transferLogArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        transferLogArea.setEditable(false);
        sendPanel.add(new JScrollPane(transferLogArea), BorderLayout.SOUTH);

        panel.add(recvPanel);
        panel.add(sendPanel);
        return panel;
    }

    // =========================================================================
    // TAB 4: EVALUATION & BENCHMARKS
    // =========================================================================
    private JPanel createMetricsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Comparison Panel
        JPanel compPanel = new JPanel(new GridBagLayout());
        compPanel.setBorder(new TitledBorder("Pairwise Audio Quality Comparison (Original vs Stego)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        compPanel.add(new JLabel("Original WAV:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        metricsOrigField = new JTextField();
        compPanel.add(metricsOrigField, gbc);
        gbc.gridx = 2; gbc.weightx = 0.0;
        JButton browseOrigBtn = new JButton("Browse...");
        browseOrigBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(new File("."));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                metricsOrigField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        compPanel.add(browseOrigBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        compPanel.add(new JLabel("Stego WAV:"), gbc);
        gbc.gridx = 1;
        metricsStegoField = new JTextField();
        compPanel.add(metricsStegoField, gbc);
        gbc.gridx = 2;
        JButton browseStegoBtn = new JButton("Browse...");
        browseStegoBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(new File("."));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                metricsStegoField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        compPanel.add(browseStegoBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        JButton calcBtn = new JButton("📊 Compute SNR, PSNR, and MSE");
        calcBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        calcBtn.addActionListener(e -> computeDirectMetrics());
        compPanel.add(calcBtn, gbc);

        // Score display cards
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 8, 8));
        cardsPanel.setBorder(new EmptyBorder(6, 4, 6, 4));

        metricsSnrVal = createMetricCard("SNR", "--- dB", cardsPanel);
        metricsPsnrVal = createMetricCard("PSNR", "--- dB", cardsPanel);
        metricsMseVal = createMetricCard("MSE", "---", cardsPanel);
        metricsModifiedVal = createMetricCard("Modified Samples", "---", cardsPanel);

        gbc.gridy = 3;
        compPanel.add(cardsPanel, gbc);

        panel.add(compPanel, BorderLayout.NORTH);

        // Systematic Benchmark Table Panel
        JPanel benchPanel = new JPanel(new BorderLayout(6, 6));
        benchPanel.setBorder(new TitledBorder("Systematic Performance & Capacity Results Table"));

        String[] columns = {"Payload (Bytes)", "LSB Bits", "SNR (dB)", "PSNR (dB)", "MSE", "Embed (ms)", "Extract (ms)", "Exact Recovery", "Carrier Usage %"};
        benchmarkTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        benchmarkTable = new JTable(benchmarkTableModel);
        benchmarkTable.setFillsViewportHeight(true);
        benchmarkTable.setRowHeight(22);
        benchPanel.add(new JScrollPane(benchmarkTable), BorderLayout.CENTER);

        JPanel benchBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton runBenchBtn = new JButton("🚀 Run Full Systematic Benchmark Suite");
        runBenchBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        runBenchBtn.setBackground(new Color(110, 40, 130));
        runBenchBtn.setForeground(Color.WHITE);
        runBenchBtn.addActionListener(e -> runSystematicBenchmark());
        benchBottom.add(runBenchBtn);
        benchPanel.add(benchBottom, BorderLayout.SOUTH);

        panel.add(benchPanel, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createMetricCard(String title, String initialVal, JPanel parent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(6, 6, 6, 6)
        ));
        card.setBackground(new Color(245, 248, 252));
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel valLbl = new JLabel(initialVal, SwingConstants.CENTER);
        valLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        valLbl.setForeground(new Color(20, 60, 110));

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        parent.add(card);
        return valLbl;
    }

    // =========================================================================
    // ACTION HANDLERS
    // =========================================================================
    private void chooseCarrierFile() {
        JFileChooser chooser = new JFileChooser(new File("."));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentCarrierFile = chooser.getSelectedFile();
            carrierPathField.setText(currentCarrierFile.getAbsolutePath());
            updateCarrierCapacityPreview();
        }
    }

    private void generateTestCarrier() {
        try {
            File temp = new File("carrier_sample.wav");
            WavAudio audio = AudioToneGenerator.generateChime(5.0, 44100, true, temp);
            currentCarrierFile = temp;
            carrierPathField.setText(temp.getAbsolutePath());
            updateCarrierCapacityPreview();
            JOptionPane.showMessageDialog(this,
                    "Generated 5.0-second 16-bit PCM WAV test file:\n" + temp.getAbsolutePath()
                            + "\nTotal Samples: " + String.format("%,d", audio.getTotalSamples()),
                    "Test Carrier Created", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to generate carrier: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCarrierCapacityPreview() {
        try {
            File f = new File(carrierPathField.getText().trim());
            if (f.exists()) {
                WavAudio audio = WavAudio.read(f);
                int bits = getSelectedBitsPerSample();
                int maxCap = LsbSteganography.calculateMaxPayloadBytes(audio.getTotalSamples(), bits);
                embedCapacityLabel.setText(String.format("Carrier: %,d samples (%.2fs) | Max Payload Capacity @ %d LSB: %,d bytes",
                        audio.getTotalSamples(), audio.getDurationSeconds(), bits, maxCap));
            }
        } catch (Exception ignored) {
        }
    }

    private int getSelectedBitsPerSample() {
        int idx = lsbDepthCombo.getSelectedIndex();
        if (idx == 1) return 2;
        if (idx == 2) return 4;
        return 1;
    }

    private void chooseOutputFile() {
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setSelectedFile(new File("stego_audio.wav"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentStegoOutputFile = chooser.getSelectedFile();
            stegoOutputPathField.setText(currentStegoOutputFile.getAbsolutePath());
        }
    }

    private void performEmbedding() {
        try {
            String carrierPath = carrierPathField.getText().trim();
            if (carrierPath.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select or generate a carrier WAV audio file first.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            File carrierFile = new File(carrierPath);
            if (!carrierFile.exists()) {
                JOptionPane.showMessageDialog(this, "Carrier file not found: " + carrierPath, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String message = secretMessageArea.getText();
            if (message.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Secret message cannot be empty.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String password = new String(embedPasswordField.getPassword());
            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Password cannot be empty.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int bits = getSelectedBitsPerSample();
            String outPath = stegoOutputPathField.getText().trim();
            File outFile = new File(outPath);

            WavAudio carrier = WavAudio.read(carrierFile);

            long t0 = System.nanoTime();
            LsbSteganography.EmbeddingResult result = LsbSteganography.embed(carrier, message, password, bits);
            long t1 = System.nanoTime();
            long embedTimeMs = (t1 - t0) / 1_000_000;

            result.getStegoAudio().save(outFile);
            currentStegoOutputFile = outFile;

            // Auto-fill Extract tab & Metrics tab
            extractStegoPathField.setText(outFile.getAbsolutePath());
            currentExtractStegoFile = outFile;
            metricsOrigField.setText(carrierFile.getAbsolutePath());
            metricsStegoField.setText(outFile.getAbsolutePath());

            AudioMetrics.MetricsResult m = AudioMetrics.evaluate(carrier, result.getStegoAudio(), embedTimeMs, 0, true);

            embedStatusLabel.setText("Status: ✔ Successfully embedded " + message.getBytes().length + " bytes into " + outFile.getName() + " in " + embedTimeMs + " ms.");
            embedCapacityLabel.setText(String.format("Carrier Usage: %.2f%% (%d / %d samples)",
                    result.getCapacityUsagePercentage(), result.getSamplesUsed(), result.getTotalCarrierSamples()));
            embedSnrLabel.setText(String.format("SNR: %.2f dB | PSNR: %.2f dB | MSE: %.4f (Inaudible Distortion)",
                    m.getSnrDb(), m.getPsnrDb(), m.getMse()));

            JOptionPane.showMessageDialog(this,
                    "Steganographic audio generated successfully!\n"
                            + "File: " + outFile.getAbsolutePath() + "\n"
                            + "SNR: " + String.format("%.2f dB", m.getSnrDb()) + "\n"
                            + "PSNR: " + String.format("%.2f dB", m.getPsnrDb()) + "\n"
                            + "Carrier Usage: " + String.format("%.2f%%", result.getCapacityUsagePercentage()),
                    "Embedding Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Embedding Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            embedStatusLabel.setText("Status: ❌ Error - " + ex.getMessage());
        }
    }

    private void chooseExtractStegoFile() {
        JFileChooser chooser = new JFileChooser(new File("."));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentExtractStegoFile = chooser.getSelectedFile();
            extractStegoPathField.setText(currentExtractStegoFile.getAbsolutePath());
        }
    }

    private void performExtraction() {
        try {
            String path = extractStegoPathField.getText().trim();
            File stegoFile = new File(path);
            if (!stegoFile.exists()) {
                JOptionPane.showMessageDialog(this, "Stego audio file not found: " + path, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String password = new String(extractPasswordField.getPassword());
            WavAudio stego = WavAudio.read(stegoFile);

            long t0 = System.nanoTime();
            String recovered = LsbSteganography.extract(stego, password);
            long t1 = System.nanoTime();
            long extractTimeMs = (t1 - t0) / 1_000_000;

            recoveredMessageArea.setText(recovered);
            extractStatusLabel.setText("Status: ✔ AES-GCM Authentication Verified! Extracted in " + extractTimeMs + " ms.");
            extractStatusLabel.setForeground(new Color(20, 120, 40));

        } catch (AuthenticationFailedException afe) {
            recoveredMessageArea.setText("");
            extractStatusLabel.setText("Status: ❌ Authentication Failed: Incorrect password or tampered audio payload!");
            extractStatusLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this,
                    "AES-GCM Authentication Failed:\n" + afe.getMessage()
                            + "\n\nThis guarantees that wrong keys or corrupted audio bits are caught immediately.",
                    "Authentication Mismatch", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            recoveredMessageArea.setText("");
            extractStatusLabel.setText("Status: ❌ Extraction Error: " + ex.getMessage());
            extractStatusLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Extraction Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void playAudioFile(File file) {
        if (file == null || !file.exists()) {
            JOptionPane.showMessageDialog(this, "Please select an existing audio file first.", "File Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            audioPlayer.play(file, () -> {
                SwingUtilities.invokeLater(() -> {
                    // playback finished
                });
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Playback Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startTcpServer() {
        try {
            int port = Integer.parseInt(serverPortField.getText().trim());
            File dir = new File(serverDirField.getText().trim());
            activeReceiver = new TcpReceiver(port, dir, new TcpTransferListener() {
                @Override
                public void onProgress(long bytesTransferred, long totalBytes, String statusMessage) {
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Server Status: " + statusMessage);
                        appendTransferLog("[Server] " + statusMessage);
                    });
                }

                @Override
                public void onComplete(String successMessage) {
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Server Status: " + successMessage);
                        appendTransferLog("[Server SUCCESS] " + successMessage);
                        JOptionPane.showMessageDialog(EchoHideGui.this, successMessage, "File Received", JOptionPane.INFORMATION_MESSAGE);
                    });
                }

                @Override
                public void onError(Exception exception) {
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Server Error: " + exception.getMessage());
                        appendTransferLog("[Server ERROR] " + exception.getMessage());
                    });
                }
            });

            activeReceiver.start();
            startServerBtn.setEnabled(false);
            stopServerBtn.setEnabled(true);
            serverStatusLabel.setText("Server Status: Listening on port " + port + "...");
            serverStatusLabel.setForeground(new Color(20, 120, 40));
            appendTransferLog("[Server Started] Listening on port " + port);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Server Startup Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopTcpServer() {
        if (activeReceiver != null) {
            activeReceiver.stop();
            activeReceiver = null;
        }
        startServerBtn.setEnabled(true);
        stopServerBtn.setEnabled(false);
        serverStatusLabel.setText("Server Status: Stopped");
        serverStatusLabel.setForeground(Color.GRAY);
        appendTransferLog("[Server Stopped]");
    }

    private void sendFileOverTcp() {
        try {
            String host = clientHostField.getText().trim();
            int port = Integer.parseInt(clientPortField.getText().trim());
            File file = new File(clientFileField.getText().trim());

            if (!file.exists()) {
                JOptionPane.showMessageDialog(this, "File to send does not exist: " + file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            transferProgressBar.setValue(0);
            appendTransferLog("[Client] Connecting to " + host + ":" + port + " to send " + file.getName());

            TcpSender.sendFile(host, port, file, new TcpTransferListener() {
                @Override
                public void onProgress(long bytesTransferred, long totalBytes, String statusMessage) {
                    SwingUtilities.invokeLater(() -> {
                        if (totalBytes > 0) {
                            int pct = (int) ((double) bytesTransferred / totalBytes * 100);
                            transferProgressBar.setValue(pct);
                        }
                        appendTransferLog("[Client] " + statusMessage);
                    });
                }

                @Override
                public void onComplete(String successMessage) {
                    SwingUtilities.invokeLater(() -> {
                        transferProgressBar.setValue(100);
                        appendTransferLog("[Client SUCCESS] " + successMessage);
                        JOptionPane.showMessageDialog(EchoHideGui.this, successMessage, "Transfer Completed", JOptionPane.INFORMATION_MESSAGE);
                    });
                }

                @Override
                public void onError(Exception exception) {
                    SwingUtilities.invokeLater(() -> {
                        appendTransferLog("[Client ERROR] " + exception.getMessage());
                        JOptionPane.showMessageDialog(EchoHideGui.this, "Transfer Failed: " + exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Sender Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void appendTransferLog(String msg) {
        transferLogArea.append(msg + "\n");
        transferLogArea.setCaretPosition(transferLogArea.getDocument().getLength());
    }

    private void computeDirectMetrics() {
        try {
            File orig = new File(metricsOrigField.getText().trim());
            File stego = new File(metricsStegoField.getText().trim());

            if (!orig.exists() || !stego.exists()) {
                JOptionPane.showMessageDialog(this, "Please select valid Original and Stego WAV files.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            WavAudio a1 = WavAudio.read(orig);
            WavAudio a2 = WavAudio.read(stego);

            AudioMetrics.MetricsResult m = AudioMetrics.evaluate(a1, a2, 0, 0, true);

            metricsSnrVal.setText(String.format("%.2f dB", m.getSnrDb()));
            metricsPsnrVal.setText(String.format("%.2f dB", m.getPsnrDb()));
            metricsMseVal.setText(String.format("%.6f", m.getMse()));
            metricsModifiedVal.setText(String.format("%,d (%.2f%%)", m.getModifiedSamplesCount(), m.getModifiedPercentage()));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Metrics Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runSystematicBenchmark() {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> benchmarkTableModel.setRowCount(0));

                File carrierFile = currentCarrierFile != null ? currentCarrierFile :
                        (!carrierPathField.getText().trim().isEmpty() ? new File(carrierPathField.getText().trim()) : null);

                WavAudio carrier = (carrierFile != null && carrierFile.exists()) ? WavAudio.read(carrierFile) : null;

                List<BenchmarkService.BenchmarkRow> rows = BenchmarkService.runBenchmark(carrier);

                SwingUtilities.invokeLater(() -> {
                    for (BenchmarkService.BenchmarkRow r : rows) {
                        benchmarkTableModel.addRow(new Object[]{
                                r.payloadBytes,
                                r.bitsPerSample,
                                String.format("%.2f", r.snrDb),
                                String.format("%.2f", r.psnrDb),
                                String.format("%.6f", r.mse),
                                r.embedTimeMs,
                                r.extractTimeMs,
                                r.recoverySuccess ? "SUCCESS" : "FAIL",
                                String.format("%.2f%%", r.carrierUsagePercent)
                        });
                    }
                    JOptionPane.showMessageDialog(EchoHideGui.this,
                            "Benchmark finished across " + rows.size() + " test scenarios!\nResults populated in the table.",
                            "Benchmark Completed", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(EchoHideGui.this, "Benchmark Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "Benchmark-Worker").start();
    }
}

