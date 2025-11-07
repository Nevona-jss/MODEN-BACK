package com.moden.modenapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.util.Base64;
import java.util.Objects;

@Configuration
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // old flat keys
    @Value("${firebase.service-account:}")
    private String serviceAccountPath;

    @Value("${firebase.credentials-base64:}")
    private String credentialsBase64Flat;

    @Value("${firebase.database-url:}")
    private String databaseUrl;

    // new nested keys
    @Value("${firebase.credentials.base64:}")
    private String credentialsBase64Nested;

    @Value("${firebase.credentials.file:}")
    private String credentialsFileNested;

    private GoogleCredentials loadCredentials() throws IOException {
        // 0) env fallbacks
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        String envB64  = System.getenv("FIREBASE_SA_BASE64");

        // 1) base64: prefer nested > flat > env
        String b64 = firstNonBlank(credentialsBase64Nested, credentialsBase64Flat, envB64);
        if (isNotBlank(b64)) {
            byte[] json = Base64.getDecoder().decode(b64);
            try (InputStream in = new ByteArrayInputStream(json)) {
                return GoogleCredentials.fromStream(in);
            }
        }

        // 2) file: prefer nested > old flat key
        String fileProp = firstNonBlank(credentialsFileNested, serviceAccountPath);
        if (isNotBlank(fileProp)) {
            Resource res = resourceLoader.getResource(fileProp);
            if (!res.exists()) {
                // Support bare classpath without prefix & plain file paths
                if (fileProp.startsWith("classpath:")) {
                    String p = fileProp.substring("classpath:".length());
                    try (InputStream in = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream(p.startsWith("/") ? p.substring(1) : p)) {
                        if (in == null) throw new FileNotFoundException("Service account not found on classpath: " + p);
                        return GoogleCredentials.fromStream(in);
                    }
                } else {
                    try (InputStream in = new FileInputStream(fileProp)) {
                        return GoogleCredentials.fromStream(in);
                    } catch (FileNotFoundException e) {
                        throw new FileNotFoundException("Service account not found: " + fileProp);
                    }
                }
            } else {
                try (InputStream in = res.getInputStream()) {
                    return GoogleCredentials.fromStream(in);
                }
            }
        }

        // 3) GOOGLE_APPLICATION_CREDENTIALS absolute path
        if (isNotBlank(envPath)) {
            try (InputStream in = new FileInputStream(envPath)) {
                return GoogleCredentials.fromStream(in);
            }
        }

        // 4) last resort: classpath default
        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("firebase-service-account.json");
        if (in != null) {
            return GoogleCredentials.fromStream(in);
        }

        throw new IllegalStateException(
                "Firebase credentials not found. Provide one of:\n" +
                        "- firebase.credentials.base64 (or firebase.credentials-base64),\n" +
                        "- firebase.credentials.file (or firebase.service-account),\n" +
                        "- or set GOOGLE_APPLICATION_CREDENTIALS env var."
        );
    }

    @Bean
    @ConditionalOnMissingBean(FirebaseApp.class)
    public FirebaseApp firebaseApp() throws IOException {
        // reuse DEFAULT if already initialized (devtools/hot-reload safe)
        for (FirebaseApp app : FirebaseApp.getApps()) {
            if (Objects.equals(app.getName(), FirebaseApp.DEFAULT_APP_NAME)) {
                return app;
            }
        }

        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(loadCredentials());

        if (isNotBlank(databaseUrl)) {
            builder.setDatabaseUrl(databaseUrl);
        }

        return FirebaseApp.initializeApp(builder.build());
    }

    @Bean
    @ConditionalOnMissingBean(FirebaseAuth.class)
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    // helpers
    private static String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }
    private static boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
}
