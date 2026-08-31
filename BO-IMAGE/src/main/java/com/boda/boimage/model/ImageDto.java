package com.boda.boimage.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ImageDto {

    private String key;
    private String url;
    private long size;
    private Instant lastModified;
}
