package com.boda.boimage.services;

import com.boda.boimage.config.S3Properties;
import com.boda.boimage.model.ImageDto;
import com.boda.boimage.model.ImageUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final long MAX_SIZE_BYTES = 4L * 1024 * 1024 * 1024;

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "video/mp4", ".mp4",
            "video/webm", ".webm",
            "video/ogg", ".ogg",
            "video/quicktime", ".mov",
            "video/x-m4v", ".m4v");

    private final S3AsyncClient s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public Mono<ImageUploadResponse> upload(FilePart file, String folder) {
        MediaType mediaType = file.headers().getContentType();
        String contentType = mediaType != null ? mediaType.toString().toLowerCase() : "";
        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
        if (extension == null) {
            return Mono.error(new IllegalArgumentException(
                    "Tipo de archivo no permitido: " + contentType + ". Permitidos: " + EXTENSION_BY_CONTENT_TYPE.keySet()));
        }
        String key = sanitizeFolder(folder) + "/" + UUID.randomUUID() + extension;

        return Mono.usingWhen(
                Mono.fromCallable(() -> Files.createTempFile("bo-image-upload-", extension)),
                tempFile -> file.transferTo(tempFile)
                        .then(Mono.fromCallable(() -> {
                            long size = Files.size(tempFile);
                            if (size == 0) {
                                throw new IllegalArgumentException("El archivo está vacío");
                            }
                            if (size > MAX_SIZE_BYTES) {
                                throw new IllegalArgumentException("El archivo supera el tamaño máximo de 4 GB");
                            }
                            return PutObjectRequest.builder()
                                    .bucket(properties.getBucket())
                                    .key(key)
                                    .contentType(contentType)
                                    .build();
                        }))
                        .flatMap(request -> Mono.fromFuture(s3Client.putObject(request, AsyncRequestBody.fromFile(tempFile))))
                        .map(response -> new ImageUploadResponse(key, presignUrl(key))),
                tempFile -> Mono.fromRunnable(() -> {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (Exception ignored) {
                    }
                }),
                (tempFile, error) -> Mono.fromRunnable(() -> {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (Exception ignored) {
                    }
                }),
                tempFile -> Mono.fromRunnable(() -> {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (Exception ignored) {
                    }
                })
        );
    }

    public Mono<List<ImageDto>> list(String folder) {
        String prefix = sanitizeFolder(folder) + "/";
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(properties.getBucket())
                .prefix(prefix)
                .build();
        return Flux.from(s3Client.listObjectsV2Paginator(request))
                .flatMapIterable(ListObjectsV2Response::contents)
                .filter(obj -> !obj.key().endsWith("/"))
                .map(obj -> new ImageDto(obj.key(), presignUrl(obj.key()), obj.size(), obj.lastModified()))
                .collectList();
    }

    public String presignUrl(String key) {
        validateKey(key);
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(properties.getPresignExpirationMinutes()))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public Mono<Void> delete(String key) {
        validateKey(key);
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build();
        return Mono.fromFuture(s3Client.deleteObject(request)).then();
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "imagenes";
        }
        String clean = folder.trim().replace("\\", "/")
                .replaceAll("[^a-zA-Z0-9/_-]", "")
                .replaceAll("/{2,}", "/")
                .replaceAll("^/+|/+$", "");
        if (clean.isBlank() || clean.contains("..")) {
            return "imagenes";
        }
        return clean;
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.contains("..")) {
            throw new IllegalArgumentException("Key no válida: " + key);
        }
    }
}
