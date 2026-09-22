package com.legalmetrology.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.supabase")
public record SupabaseStorageProperties(
        String url,
        String serviceRoleKey,
        Storage storage
) {
    public record Storage(
            String bucketImages,
            String bucketReports,
            String bucketEvidence,
            String bucketTemp,
            String bucketExports,
            String bucketTrainingData,
            long signedUrlExpirySeconds
    ) {
    }
}
