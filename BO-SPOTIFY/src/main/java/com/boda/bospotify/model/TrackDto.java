package com.boda.bospotify.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackDto {

    private String id;
    private String name;
    private List<String> artists;
    private String album;
    private String image;
    private Long durationMs;
    private String uri;
    private String previewUrl;
}
