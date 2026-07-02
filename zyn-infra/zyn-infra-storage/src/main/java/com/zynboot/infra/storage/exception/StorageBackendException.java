package com.zynboot.infra.storage.exception;

public class StorageBackendException extends StorageException {

    public StorageBackendException(String message) {
        super(message);
    }

    public StorageBackendException(String message, Throwable cause) {
        super(message, cause);
    }
}
