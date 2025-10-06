package org.sky.function.exception;

public class KeyVaultException extends RuntimeException {
    public KeyVaultException(String message, Throwable cause) {
        super(message, cause);
    }
    public KeyVaultException(String message) {
        super(message);
    }
}

