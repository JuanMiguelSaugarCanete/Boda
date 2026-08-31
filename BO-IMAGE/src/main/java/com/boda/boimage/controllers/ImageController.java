package com.boda.boimage.controllers;

import com.boda.boimage.model.ImageDto;
import com.boda.boimage.model.ImageUploadResponse;
import com.boda.boimage.services.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private static final int MAX_FILES_PER_BATCH = 20;

    private final S3Service s3Service;

    // 1. Subir un archivo multimedia (multipart, campo "file") a una carpeta del bucket
    @PostMapping
    public Mono<ResponseEntity<ImageUploadResponse>> upload(@RequestPart("file") FilePart file,
                                                            @RequestParam(defaultValue = "imagenes") String folder) {
        return s3Service.upload(file, folder)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().build()));
    }

    // 1b. Subir varios archivos multimedia a la vez (multipart, campo "files") a una carpeta del bucket
    @PostMapping("/batch")
    public Mono<ResponseEntity<?>> uploadBatch(@RequestPart("files") Flux<FilePart> files,
                                               @RequestParam(defaultValue = "imagenes") String folder) {
        return files.collectList()
                .flatMap(list -> {
                    if (list.isEmpty() || list.size() > MAX_FILES_PER_BATCH) {
                        return Mono.just(ResponseEntity.badRequest().build());
                    }
                    return Flux.fromIterable(list)
                            .flatMap(file -> s3Service.upload(file, folder)
                                    .onErrorResume(IllegalArgumentException.class, e -> Mono.empty()))
                            .collectList()
                            .flatMap(uploaded -> {
                                if (uploaded.isEmpty()) {
                                    return Mono.just(ResponseEntity.badRequest().build());
                                }
                                return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(uploaded));
                            });
                });
    }

    // 2. Listar los archivos de una carpeta (cada uno con su URL prefirmada)
    @GetMapping
    public Mono<ResponseEntity<List<ImageDto>>> list(@RequestParam(defaultValue = "imagenes") String folder) {
        return s3Service.list(folder)
                .map(ResponseEntity::ok);
    }

    // 3. Generar una URL prefirmada nueva para una key existente
    @GetMapping("/url")
    public Mono<ResponseEntity<Map<String, String>>> presignedUrl(@RequestParam String key) {
        return Mono.fromCallable(() -> s3Service.presignUrl(key))
                .map(url -> ResponseEntity.ok(Map.of("key", key, "url", url)))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().build()));
    }

    // 4. Borrar un archivo del bucket
    @DeleteMapping
    public Mono<ResponseEntity<Void>> delete(@RequestParam String key) {
        return s3Service.delete(key)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().<Void>build()));
    }
}
