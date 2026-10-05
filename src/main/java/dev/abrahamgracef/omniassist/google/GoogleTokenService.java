package dev.abrahamgracef.omniassist.google;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenService {

    public String getAccessToken(OAuth2AuthorizedClient client) {
        return client.getAccessToken().getTokenValue();
    }
}