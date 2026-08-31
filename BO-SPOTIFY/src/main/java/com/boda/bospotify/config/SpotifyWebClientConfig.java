package com.boda.bospotify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SpotifyWebClientConfig {

    @Bean
    public WebClient spotifyApiWebClient() {
        return WebClient.create("https://api.spotify.com/v1");
    }

    @Bean
    public WebClient spotifyAccountsWebClient() {
        return WebClient.create("https://accounts.spotify.com");
    }
}
