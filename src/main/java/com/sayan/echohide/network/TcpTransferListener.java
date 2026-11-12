package com.sayan.echohide.network;

/**
 * Callback listener for TCP file transfer progress and status updates.
 */
public interface TcpTransferListener {
    void onProgress(long bytesTransferred, long totalBytes, String statusMessage);
    void onComplete(String successMessage);
    void onError(Exception exception);
}

