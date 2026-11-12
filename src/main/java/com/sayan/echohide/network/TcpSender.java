package com.sayan.echohide.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Sends a stego-WAV file to a receiver using a length-framed TCP protocol.
 */
public class TcpSender {

    public static final byte[] PROTOCOL_MAGIC = new byte[]{'E', 'C', 'H', 'O'};

    /**
     * Sends the specified file over TCP to host:port.
     */
    public static void sendFile(String host, int port, File file, TcpTransferListener listener) {
        new Thread(() -> {
            try {
                if (!file.exists() || !file.isFile()) {
                    throw new IOException("File does not exist or is not a regular file: " + file.getAbsolutePath());
                }

                long fileSize = file.length();
                byte[] nameBytes = file.getName().getBytes(StandardCharsets.UTF_8);

                if (listener != null) {
                    listener.onProgress(0, fileSize, "Connecting to " + host + ":" + port + "...");
                }

                try (Socket socket = new Socket(host, port);
                     DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                     DataInputStream dis = new DataInputStream(socket.getInputStream());
                     FileInputStream fis = new FileInputStream(file)) {

                    // 1. Send Magic
                    dos.write(PROTOCOL_MAGIC);

                    // 2. Send Filename length + name
                    dos.writeShort(nameBytes.length);
                    dos.write(nameBytes);

                    // 3. Send file size (8 bytes)
                    dos.writeLong(fileSize);
                    dos.flush();

                    // 4. Send payload in 8KB chunks
                    byte[] buffer = new byte[8192];
                    long totalSent = 0;
                    int read;

                    while ((read = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, read);
                        totalSent += read;
                        if (listener != null) {
                            listener.onProgress(totalSent, fileSize,
                                    String.format("Sending: %d / %d KB (%.1f%%)",
                                            totalSent / 1024, fileSize / 1024,
                                            (double) totalSent / fileSize * 100.0));
                        }
                    }
                    dos.flush();

                    // 5. Await ACK byte from receiver
                    int ack = dis.read();
                    if (ack != 0x06) {
                        throw new IOException("Receiver did not acknowledge transfer (received code: " + ack + ")");
                    }

                    if (listener != null) {
                        listener.onComplete("Transfer completed successfully! File sent to " + host + ":" + port);
                    }
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        }, "TcpSender-Thread").start();
    }
}

