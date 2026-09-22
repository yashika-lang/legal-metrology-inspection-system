package com.legalmetrology.exception;

/** Thrown when a Supabase Storage upload/download/delete operation fails. */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
