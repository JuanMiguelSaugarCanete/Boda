package com.boda.bospotify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spotify")
public class SpotifyProperties {

    private String clientId;
    private String clientSecret;
    private String playlistId;
    private String redirectUri;
    private String refreshToken;
}
