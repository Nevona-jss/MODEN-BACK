package com.moden.modenapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // Nested (yaml: firebase.credentials.base64 / file)
    @Value("${firebase.credentials.base64:}")
    private String credentialsBase64Nested;

    @Value("${firebase.credentials.file:}")
    private String credentialsFileNested;

    // Flat (yaml: firebase.credentials-base64 / service-account) — ixtiyoriy qo‘shimcha aliaslar
    @Value("${firebase.credentials-base64:}")
    private String credentialsBase64Flat;

    @Value("${firebase.service-account:}")
    private String serviceAccountPath;

    // Optional: Realtime DB/Storage URL kerak bo‘lsa
    @Value("${firebase.database-url:}")
    private String databaseUrl;

    @Bean
    @ConditionalOnMissingBean(FirebaseApp.class)
    public FirebaseApp firebaseApp() throws IOException {
        // DEFAULT_APP oldin yaratilgan bo‘lsa — qayta ishlatmang
        for (FirebaseApp app : FirebaseApp.getApps()) {
            if (Objects.equals(app.getName(), FirebaseApp.DEFAULT_APP_NAME)) {
                log.info("[Firebase] Reusing existing DEFAULT app");
                return app;
            }
        }

        GoogleCredentials credentials = loadCredentials();

        FirebaseOptions.Builder builder = FirebaseOptions.builder().setCredentials(credentials);
        if (isNotBlank(databaseUrl)) builder.setDatabaseUrl(databaseUrl);

        FirebaseApp app = FirebaseApp.initializeApp(builder.build());
        log.info("[Firebase] Initialized DEFAULT app successfully");
        return app;
    }

    @Bean
    @ConditionalOnMissingBean(FirebaseAuth.class)
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    // -------------------- helpers --------------------

    private GoogleCredentials loadCredentials() throws IOException {
        // Env fallbacks
        String envB64  = System.getenv("FIREBASE_SA_BASE64");
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");

        // 1) Base64 (nested > flat > env)
        String b64 = firstNonBlank(credentialsBase64Nested, credentialsBase64Flat, envB64);
        if (isNotBlank(b64)) {
            log.info("[Firebase] Loading credentials from Base64 (property/env)");
            byte[] json = Base64.getDecoder().decode(b64);
            try (InputStream in = new ByteArrayInputStream(json)) {
                return GoogleCredentials.fromStream(in);
            }
        }

        // 2) File (nested > flat)
        String fileProp = firstNonBlank(credentialsFileNested, serviceAccountPath);
        if (isNotBlank(fileProp)) {
            // ResourceLoader ga berib ko‘ramiz (classpath:, file:, https:, va h.k.)
            Resource res = resourceLoader.getResource(fileProp);
            if (res.exists()) {
                log.info("[Firebase] Loading credentials from resource: {}", fileProp);
                try (InputStream in = res.getInputStream()) {
                    return GoogleCredentials.fromStream(in);
                }
            }
            // Agar prefix yo‘q bo‘lsa, classpath sifatida ham urinamiz
            if (!fileProp.contains(":")) {
                InputStream cp = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(fileProp);
                if (cp != null) {
                    log.info("[Firebase] Loading credentials from classpath: {}", fileProp);
                    return GoogleCredentials.fromStream(cp);
                }
            }
            // Oxirgi urinish — local fayl yo‘li
            File f = new File(fileProp);
            if (f.exists()) {
                log.info("[Firebase] Loading credentials from file path: {}", f.getAbsolutePath());
                try (InputStream in = new FileInputStream(f)) {
                    return GoogleCredentials.fromStream(in);
                }
            }
            throw new FileNotFoundException("Firebase service account not found: " + fileProp);
        }

        // 3) GOOGLE_APPLICATION_CREDENTIALS (absolute file path)
        if (isNotBlank(envPath)) {
            File f = new File(envPath);
            if (f.exists()) {
                log.info("[Firebase] Loading credentials from GOOGLE_APPLICATION_CREDENTIALS: {}", envPath);
                try (InputStream in = new FileInputStream(f)) {
                    return GoogleCredentials.fromStream(in);
                }
            } else {
                log.warn("[Firebase] GOOGLE_APPLICATION_CREDENTIALS set but file not found: {}", envPath);
            }
        }

        // 4) Classpath default
        String defaultCp = "firebase/serviceAccount.json";
        InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(defaultCp);
        if (in != null) {
            log.info("[Firebase] Loading credentials from classpath default: {}", defaultCp);
            return GoogleCredentials.fromStream(in);
        }

        throw new IllegalStateException(
                "Firebase credentials not found. Provide one of:\n" +
                        " - firebase.credentials.base64  (or env FIREBASE_SA_BASE64),\n" +
                        " - firebase.credentials.file    (or firebase.service-account),\n" +
                        " - or env GOOGLE_APPLICATION_CREDENTIALS (absolute file path),\n" +
                        " - or include classpath resource firebase/serviceAccount.json"
        );
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (isNotBlank(v)) return v;
        return null;
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
