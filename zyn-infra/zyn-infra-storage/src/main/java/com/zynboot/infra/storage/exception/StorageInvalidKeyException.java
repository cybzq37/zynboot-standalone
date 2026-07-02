package com.zynboot.infra.storage.exception;

public class StorageInvalidKeyException extends IllegalArgumentException {

    public StorageInvalidKeyException(String message) {
        super(message);
    }

    public StorageInvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
