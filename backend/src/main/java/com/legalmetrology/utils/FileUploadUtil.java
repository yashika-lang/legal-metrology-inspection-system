package com.legalmetrology.utils;

import com.legalmetrology.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class FileUploadUtil {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/heic");

    private static final long MAX_IMAGE_SIZE_BYTES = 15L * 1024 * 1024; // 15MB

    /** A safe object-key path segment: letters/digits only, 1-5 characters — never re-derived unsanitized from a client-supplied filename. */
    private static final Pattern SAFE_EXTENSION = Pattern.compile("^[a-zA-Z0-9]{1,5}$");

    private FileUploadUtil() {
    }

    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BadRequestException("Image exceeds the maximum allowed size of 15MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported image type. Allowed types: JPEG, PNG, WEBP, HEIC");
        }
    }

    /**
     * The client-supplied original filename is untrusted input used only to
     * derive a display-facing extension — it is never taken as-is. A
     * filename such as {@code "a./../../evil"} would otherwise yield the
     * substring {@code "/evil"} as its "extension" (the last {@code '.'}
     * in the string sits inside a {@code ../} segment), which — spliced
     * into a generated storage path — turns one path segment into several
     * and lets an upload escape its intended {@code {inspectionId}/{type}/}
     * folder in the storage bucket. Falling back to the (already
     * content-type-validated) MIME type whenever the filename's extension
     * doesn't match a safe allowlist closes that off entirely.
     */
    public static String extractExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            String candidate = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (SAFE_EXTENSION.matcher(candidate).matches()) {
                return candidate;
            }
        }
        return inferExtensionFromContentType(file.getContentType());
    }

    public static String generateUniqueFileName(MultipartFile file) {
        return UUID.randomUUID() + "." + extractExtension(file);
    }

    /** Infers a content type from a stored object's file extension — for re-reading an already-uploaded file back out of storage, where no MultipartFile/original content-type header is available. */
    public static String inferMimeTypeFromPath(String storagePath) {
        int lastDot = storagePath.lastIndexOf('.');
        String extension = lastDot >= 0 ? storagePath.substring(lastDot + 1).toLowerCase(Locale.ROOT) : "";
        return switch (extension) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            default -> "image/jpeg";
        };
    }

    private static String inferExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return "bin";
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/heic" -> "heic";
            default -> "bin";
        };
    }
}
