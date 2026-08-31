package com.boda.bospotify.services;

import com.boda.bospotify.config.SpotifyProperties;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyAuthService {

    private final SpotifyProperties properties;
    private final WebClient spotifyAccountsWebClient;

    private final AtomicReference<Mono<String>> clientTokenCache = new AtomicReference<>();

    private volatile String userAccessToken;
    private volatile String userRefreshToken;
    private volatile Instant userTokenExpiry = Instant.EPOCH;

    // ---------- Client Credentials (lectura de playlist y búsqueda) ----------

    public Mono<String> getClientCredentialsToken() {
        return clientTokenCache.updateAndGet(cached ->
                cached != null ? cached : requestClientCredentialsToken());
    }

    private Mono<String> requestClientCredentialsToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        return spotifyAccountsWebClient.post()
                .uri("/api/token")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("access_token").asText())
                .doOnError(e -> clientTokenCache.set(null))
                .cache(Duration.ofMinutes(55));
    }

    // ---------- Authorization Code (añadir canciones) ----------

    public String getAuthorizeUrl() {
        return UriComponentsBuilder.fromUriString("https://accounts.spotify.com/authorize")
                .queryParam("client_id", properties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("scope", "playlist-modify-public playlist-modify-private")
                .build()
                .encode()
                .toUriString();
    }

    public Mono<String> exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", properties.getRedirectUri());

        return spotifyAccountsWebClient.post()
                .uri("/api/token")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    this.userAccessToken = json.get("access_token").asText();
                    this.userTokenExpiry = Instant.now().plusSeconds(json.get("expires_in").asLong() - 60);
                    if (json.hasNonNull("refresh_token")) {
                        this.userRefreshToken = json.get("refresh_token").asText();
                    }
                    log.info("Spotify autorizado correctamente. Refresh token obtenido.");
                    return this.userRefreshToken;
                });
    }

    public Mono<String> getUserAccessToken() {
        if (userAccessToken != null && Instant.now().isBefore(userTokenExpiry)) {
            return Mono.just(userAccessToken);
        }
        String refreshToken = userRefreshToken != null ? userRefreshToken : properties.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new IllegalStateException(
                    "Spotify no autorizado. Visita /api/spotify/login para autorizar la aplicación."));
        }
        return refreshUserToken(refreshToken);
    }

    private Mono<String> refreshUserToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        return spotifyAccountsWebClient.post()
                .uri("/api/token")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    this.userAccessToken = json.get("access_token").asText();
                    this.userTokenExpiry = Instant.now().plusSeconds(json.get("expires_in").asLong() - 60);
                    if (json.hasNonNull("refresh_token")) {
                        this.userRefreshToken = json.get("refresh_token").asText();
                    }
                    return this.userAccessToken;
                });
    }

    private String basicAuthHeader() {
        String credentials = properties.getClientId() + ":" + properties.getClientSecret();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
