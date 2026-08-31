package com.boda.bospotify.services;

import com.boda.bospotify.config.SpotifyProperties;
import com.boda.bospotify.exceptions.DuplicateTrackException;
import com.boda.bospotify.model.PlaylistDto;
import com.boda.bospotify.model.TrackDto;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final SpotifyProperties properties;
    private final SpotifyAuthService authService;
    private final WebClient spotifyApiWebClient;

    public Mono<PlaylistDto> getPlaylist() {
        Mono<JsonNode> metadata = authService.getClientCredentialsToken()
                .flatMap(token -> spotifyApiWebClient.get()
                        .uri("/playlists/{id}", properties.getPlaylistId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(JsonNode.class));

        // La API actual ya no devuelve los tracks en /playlists/{id};
        // hay que pedirlos en /playlists/{id}/items con token de usuario.
        Mono<JsonNode> items = authService.getUserAccessToken()
                .flatMap(token -> spotifyApiWebClient.get()
                        .uri("/playlists/{id}/items", properties.getPlaylistId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(JsonNode.class));

        return Mono.zip(metadata, items, this::toPlaylistDto);
    }

    public Mono<List<TrackDto>> searchTracks(String query) {
        return authService.getClientCredentialsToken()
                .flatMap(token -> spotifyApiWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/search")
                                .queryParam("q", query)
                                .queryParam("type", "track")
                                .queryParam("limit", 10)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(JsonNode.class))
                .map(json -> {
                    List<TrackDto> tracks = new ArrayList<>();
                    JsonNode items = json.path("tracks").path("items");
                    if (items.isArray()) {
                        for (JsonNode item : items) {
                            tracks.add(toTrackDto(item));
                        }
                    }
                    return tracks;
                });
    }

    public Mono<Void> addTrackToPlaylist(String trackUri) {
        return getPlaylistTrackUris()
                .flatMap(uris -> {
                    if (uris.contains(trackUri)) {
                        return Mono.error(new DuplicateTrackException(trackUri));
                    }
                    return authService.getUserAccessToken()
                            .flatMap(token -> spotifyApiWebClient.post()
                                    .uri("/playlists/{id}/items", properties.getPlaylistId())
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                    .bodyValue(Map.of("uris", List.of(trackUri)))
                                    .retrieve()
                                    .bodyToMono(JsonNode.class)
                                    .then());
                });
    }

    private Mono<List<String>> getPlaylistTrackUris() {
        return authService.getUserAccessToken()
                .flatMap(token -> spotifyApiWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/playlists/{id}/items")
                                .queryParam("limit", 100)
                                .build(properties.getPlaylistId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(JsonNode.class))
                .map(json -> {
                    List<String> uris = new ArrayList<>();
                    JsonNode items = json.path("items");
                    if (items.isArray()) {
                        for (JsonNode item : items) {
                            JsonNode track = item.path("track");
                            if (track.hasNonNull("uri")) {
                                uris.add(track.path("uri").asText());
                            }
                        }
                    }
                    return uris;
                });
    }

    private PlaylistDto toPlaylistDto(JsonNode json, JsonNode itemsJson) {
        List<TrackDto> tracks = new ArrayList<>();
        JsonNode items = itemsJson.path("items");
        if (items.isArray()) {
            for (JsonNode item : items) {
                JsonNode track = item.path("item");
                if (track.hasNonNull("id") && "track".equals(track.path("type").asText())) {
                    tracks.add(toTrackDto(track));
                }
            }
        }
        return new PlaylistDto(
                json.path("id").asText(null),
                json.path("name").asText(null),
                json.path("description").asText(null),
                firstImage(json.path("images")),
                tracks
        );
    }

    private TrackDto toTrackDto(JsonNode track) {
        List<String> artists = new ArrayList<>();
        JsonNode artistsNode = track.path("artists");
        if (artistsNode.isArray()) {
            for (JsonNode artist : artistsNode) {
                artists.add(artist.path("name").asText());
            }
        }
        return new TrackDto(
                track.path("id").asText(null),
                track.path("name").asText(null),
                artists,
                track.path("album").path("name").asText(null),
                firstImage(track.path("album").path("images")),
                track.path("duration_ms").asLong(),
                track.path("uri").asText(null),
                track.path("preview_url").asText(null)
        );
    }

    private String firstImage(JsonNode images) {
        if (images.isArray() && !images.isEmpty()) {
            return images.get(0).path("url").asText(null);
        }
        return null;
    }
}
