package com.boda.bospotify.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDto {

    private String id;
    private String name;
    private String description;
    private String image;
    private List<TrackDto> tracks;
}
