package com.legalmetrology.exception;

/**
 * Every registered OCR provider failed to extract text for a given image —
 * an upstream dependency failure (API outage, quota, billing not enabled,
 * missing native library), never a malformed client request. Mirrors how
 * {@link FileStorageException} is handled: a 502, not a 400, so a caller
 * can tell "your request was fine, an external service is down" apart from
 * "you sent something invalid."
 */
public class OcrUnavailableException extends RuntimeException {
    public OcrUnavailableException(String message) {
        super(message);
    }
}
