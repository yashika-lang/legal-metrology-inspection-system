package com.legalmetrology.storage.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalmetrology.exception.FileStorageException;
import com.legalmetrology.exception.ServiceNotConfiguredException;
import com.legalmetrology.storage.config.SupabaseStorageProperties;
import com.legalmetrology.storage.dto.StorageBucket;
import com.legalmetrology.storage.dto.StorageUploadResult;
import com.legalmetrology.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final WebClient supabaseStorageWebClient;
    private final SupabaseStorageProperties properties;

    @Override
    public StorageUploadResult upload(StorageBucket bucket, String path, byte[] content, String contentType) {
        ensureConfigured();
        String bucketName = resolveBucketName(bucket);
        try {
            supabaseStorageWebClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/object/{bucket}/").path(path).build(bucketName))
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                    .header("x-upsert", "true")
                    .bodyValue(content)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Uploaded object to Supabase Storage: bucket={} path={} sizeBytes={}", bucketName, path, content.length);
            return new StorageUploadResult(bucket, path, contentType, content.length);
        } catch (WebClientException ex) {
            throw storageException("upload", bucketName, path, ex);
        }
    }

    @Override
    public byte[] download(StorageBucket bucket, String path) {
        ensureConfigured();
        String bucketName = resolveBucketName(bucket);
        try {
            return supabaseStorageWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/object/{bucket}/").path(path).build(bucketName))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (WebClientException ex) {
            throw storageException("download", bucketName, path, ex);
        }
    }

    @Override
    public void delete(StorageBucket bucket, String path) {
        ensureConfigured();
        String bucketName = resolveBucketName(bucket);
        try {
            supabaseStorageWebClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/object/{bucket}", bucketName)
                    .bodyValue(Map.of("prefixes", new String[]{path}))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Deleted object from Supabase Storage: bucket={} path={}", bucketName, path);
        } catch (WebClientException ex) {
            throw storageException("delete", bucketName, path, ex);
        }
    }

    @Override
    public String generateSignedUrl(StorageBucket bucket, String path) {
        ensureConfigured();
        String bucketName = resolveBucketName(bucket);
        try {
            JsonNode response = supabaseStorageWebClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/object/sign/{bucket}/").path(path).build(bucketName))
                    .bodyValue(Map.of("expiresIn", properties.storage().signedUrlExpirySeconds()))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.hasNonNull("signedURL")) {
                throw new FileStorageException("Supabase Storage did not return a signed URL for " + bucketName + "/" + path);
            }
            return properties.url() + "/storage/v1" + response.get("signedURL").asText();
        } catch (WebClientException ex) {
            throw storageException("generate a signed URL for", bucketName, path, ex);
        }
    }

    @Override
    public StorageUploadResult uploadTemp(byte[] content, String contentType, String suggestedFileName) {
        String path = UUID.randomUUID() + "-" + (suggestedFileName != null ? suggestedFileName : "file");
        return upload(StorageBucket.TEMP, path, content, contentType);
    }

    @Override
    public void deleteTemp(String path) {
        delete(StorageBucket.TEMP, path);
    }

    /** Checked at the start of every operation, not at startup — a backend with no Supabase credentials configured yet must still serve every non-storage endpoint normally. */
    private void ensureConfigured() {
        if (properties.url() == null || properties.url().isBlank()
                || properties.serviceRoleKey() == null || properties.serviceRoleKey().isBlank()) {
            throw new ServiceNotConfiguredException(
                    "Supabase Storage is not configured — set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY.");
        }
    }

    private String resolveBucketName(StorageBucket bucket) {
        var storage = properties.storage();
        return switch (bucket) {
            case IMAGES -> storage.bucketImages();
            case REPORTS -> storage.bucketReports();
            case EVIDENCE -> storage.bucketEvidence();
            case TEMP -> storage.bucketTemp();
            case EXPORTS -> storage.bucketExports();
            case TRAINING_DATA -> storage.bucketTrainingData();
        };
    }

    /**
     * Handles both failure shapes the reactive client throws: a
     * {@link WebClientResponseException} (Supabase answered with an error
     * status) and the narrower {@link WebClientRequestException} (the
     * request never got a response at all — DNS failure, connection
     * refused, timeout). Both must surface as the same clean
     * {@link FileStorageException} instead of leaking a raw Reactor/Netty
     * stack trace to the global exception handler.
     */
    private FileStorageException storageException(String action, String bucket, String path, WebClientException ex) {
        if (ex instanceof WebClientResponseException responseEx) {
            log.error("Failed to {} object in Supabase Storage: bucket={} path={} status={} body={}",
                    action, bucket, path, responseEx.getStatusCode(), responseEx.getResponseBodyAsString());
        } else {
            log.error("Failed to {} object in Supabase Storage: bucket={} path={} — request never reached Supabase ({})",
                    action, bucket, path, ex.getMessage());
        }
        return new FileStorageException("Failed to " + action + " object in storage bucket '" + bucket + "'", ex);
    }
}
