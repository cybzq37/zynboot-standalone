package com.zynboot.infra.storage.exception;

public class StorageConflictException extends StorageException {

    public StorageConflictException(String message) {
        super(message);
    }

    public StorageConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
