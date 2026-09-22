package com.legalmetrology.storage.dto;

public record StorageUploadResult(
        StorageBucket bucket,
        String path,
        String contentType,
        long sizeBytes
) {
}
