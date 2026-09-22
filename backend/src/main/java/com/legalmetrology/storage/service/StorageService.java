package com.legalmetrology.storage.service;

import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.dto.StorageUploadResult;

/**
 * Abstraction over Supabase Storage. Every caller works in terms of a
 * {@link StorageBucket} + object path — nobody outside this module talks
 * to the Supabase Storage REST API directly, so the underlying provider
 * could be swapped later without touching business code.
 */
public interface StorageService {

    StorageUploadResult upload(StorageBucket bucket, String path, byte[] content, String contentType);

    byte[] download(StorageBucket bucket, String path);

    void delete(StorageBucket bucket, String path);

    /** A time-limited URL a client can use to fetch a private object directly, without proxying bytes through the API. */
    String generateSignedUrl(StorageBucket bucket, String path);

    /** Convenience for the upload-before-an-inspection-exists flow — always targets the temp bucket. */
    StorageUploadResult uploadTemp(byte[] content, String contentType, String suggestedFileName);

    void deleteTemp(String path);
}
