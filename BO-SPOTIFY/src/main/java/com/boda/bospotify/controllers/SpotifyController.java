package com.boda.bospotify.controllers;

import com.boda.bospotify.exceptions.DuplicateTrackException;
import com.boda.bospotify.model.PlaylistDto;
import com.boda.bospotify.model.TrackDto;
import com.boda.bospotify.services.SpotifyAuthService;
import com.boda.bospotify.services.SpotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private final SpotifyService spotifyService;
    private final SpotifyAuthService authService;

    // 1. Setup único: redirige al consent screen de Spotify
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authService.getAuthorizeUrl()))
                .build();
    }

    // 2. Setup único: callback de Spotify con el código de autorización
    @GetMapping("/callback")
    public Mono<ResponseEntity<String>> callback(@RequestParam(required = false) String code,
                                                 @RequestParam(required = false) String error) {
        if (error != null) {
            return Mono.just(ResponseEntity.badRequest()
                    .body("Error de autorización de Spotify: " + error));
        }
        if (code == null || code.isBlank()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body("Falta el parámetro 'code'."));
        }
        return authService.exchangeCode(code)
                .map(refreshToken -> ResponseEntity.ok(
                        "Spotify autorizado correctamente. Ya puedes añadir canciones a la playlist.\n"
                                + "Refresh token (guárdalo en SPOTIFY_REFRESH_TOKEN para sobrevivir reinicios):\n"
                                + refreshToken))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body("Error al intercambiar el código: " + e.getMessage())));
    }

    // 3. Obtener la playlist con sus canciones
    @GetMapping("/playlist")
    public Mono<ResponseEntity<PlaylistDto>> getPlaylist() {
        return spotifyService.getPlaylist()
                .map(ResponseEntity::ok);
    }

    // 4. Buscar canciones en Spotify
    @GetMapping("/search")
    public Mono<ResponseEntity<List<TrackDto>>> search(@RequestParam String q) {
        return spotifyService.searchTracks(q)
                .map(ResponseEntity::ok);
    }

    // 5. Añadir una canción a la playlist
    @PostMapping("/playlist/tracks")
    public Mono<ResponseEntity<Void>> addTrack(@RequestBody Map<String, String> body) {
        String uri = body.get("uri");
        if (uri == null || uri.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return spotifyService.addTrackToPlaylist(uri)
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).<Void>build()))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()))
                .onErrorResume(DuplicateTrackException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }
}
