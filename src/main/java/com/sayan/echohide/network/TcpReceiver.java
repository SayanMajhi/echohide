package com.sayan.echohide.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Listens for incoming stego-WAV files over a length-framed TCP socket.
 */
public class TcpReceiver {

    private final int port;
    private final File saveDirectory;
    private final TcpTransferListener listener;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public TcpReceiver(int port, File saveDirectory, TcpTransferListener listener) {
        this.port = port;
        this.saveDirectory = saveDirectory;
        this.listener = listener;
    }

    public synchronized void start() throws IOException {
        if (running) return;

        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }

        serverSocket = new ServerSocket(port);
        running = true;

        new Thread(() -> {
            if (listener != null) {
                listener.onProgress(0, 0, "Listening on port " + port + "... Waiting for sender.");
            }

            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                } catch (IOException e) {
                    if (running && listener != null) {
                        listener.onError(e);
                    }
                }
            }
        }, "TcpReceiver-ServerThread").start();
    }

    private void handleClient(Socket socket) {
        new Thread(() -> {
            try (Socket s = socket;
                 DataInputStream dis = new DataInputStream(s.getInputStream());
                 DataOutputStream dos = new DataOutputStream(s.getOutputStream())) {

                // 1. Read Magic
                byte[] magic = new byte[4];
                dis.readFully(magic);
                if (!Arrays.equals(magic, TcpSender.PROTOCOL_MAGIC)) {
                    throw new IOException("Invalid protocol header from client: " + Arrays.toString(magic));
                }

                // 2. Read filename
                short nameLength = dis.readShort();
                byte[] nameBytes = new byte[nameLength];
                dis.readFully(nameBytes);
                String originalFileName = new String(nameBytes, StandardCharsets.UTF_8);

                // Sanitize file name
                String safeName = new File(originalFileName).getName();
                File targetFile = new File(saveDirectory, safeName);

                // If file exists, append timestamp or counter
                if (targetFile.exists()) {
                    String base = safeName.contains(".") ? safeName.substring(0, safeName.lastIndexOf('.')) : safeName;
                    String ext = safeName.contains(".") ? safeName.substring(safeName.lastIndexOf('.')) : "";
                    targetFile = new File(saveDirectory, base + "_" + System.currentTimeMillis() + ext);
                }

                // 3. Read total size
                long fileSize = dis.readLong();

                if (listener != null) {
                    listener.onProgress(0, fileSize, "Receiving file '" + targetFile.getName() + "' (" + (fileSize / 1024) + " KB)...");
                }

                // 4. Stream data to file
                try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    long totalReceived = 0;

                    while (totalReceived < fileSize) {
                        int toRead = (int) Math.min(buffer.length, fileSize - totalReceived);
                        int read = dis.read(buffer, 0, toRead);
                        if (read == -1) {
                            throw new IOException("Connection closed prematurely by sender.");
                        }
                        fos.write(buffer, 0, read);
                        totalReceived += read;

                        if (listener != null) {
                            listener.onProgress(totalReceived, fileSize,
                                    String.format("Receiving: %d / %d KB (%.1f%%)",
                                            totalReceived / 1024, fileSize / 1024,
                                            (double) totalReceived / fileSize * 100.0));
                        }
                    }
                    fos.flush();
                }

                // 5. Send ACK byte (0x06)
                dos.write(0x06);
                dos.flush();

                if (listener != null) {
                    listener.onComplete("Received and saved: " + targetFile.getAbsolutePath());
                }

            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        }, "TcpReceiver-ClientHandler").start();
    }

    public synchronized void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public boolean isRunning() {
        return running;
    }
}

