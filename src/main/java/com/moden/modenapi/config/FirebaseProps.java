package com.moden.modenapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "firebase")
public class FirebaseProps {
    private String projectId;
    private Credentials credentials = new Credentials();

    @Getter @Setter
    public static class Credentials {
        /** classpath:, file:, http: ... Spring Resource format */
        private String file;
        /** base64-encoded service-account JSON (optional) */
        private String base64;
    }
}
