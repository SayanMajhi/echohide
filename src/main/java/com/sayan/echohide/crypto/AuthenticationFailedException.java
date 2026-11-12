package com.sayan.echohide.crypto;

/**
 * Thrown when AES-GCM authentication tag verification fails (wrong key or corrupted data).
 */
public class AuthenticationFailedException extends Exception {
    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

