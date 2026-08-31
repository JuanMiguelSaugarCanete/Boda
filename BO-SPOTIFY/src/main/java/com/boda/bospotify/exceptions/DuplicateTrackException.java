package com.boda.bospotify.exceptions;

public class DuplicateTrackException extends RuntimeException {

    public DuplicateTrackException(String trackUri) {
        super("La canción ya está en la playlist: " + trackUri);
    }
}
