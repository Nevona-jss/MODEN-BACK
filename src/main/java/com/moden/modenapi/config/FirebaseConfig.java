package com.moden.modenapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(FirebaseProps.class)
public class FirebaseConfig {

    private final FirebaseProps props;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        GoogleCredentials cred = loadCredentials();  // props'dan yuklaymiz
        FirebaseOptions opts = FirebaseOptions.builder()
                .setCredentials(cred)
                .setProjectId(props.getProjectId())
                .build();
        return FirebaseApp.initializeApp(opts);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        // 1) base64
        String b64 = Optional.ofNullable(props.getCredentials().getBase64())
                .map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
        if (b64 != null) {
            byte[] decoded = Base64.getDecoder().decode(b64);
            return GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
        }
        // 2) file (Spring Resource, masalan classpath:/...)
        String loc = Optional.ofNullable(props.getCredentials().getFile())
                .map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
        if (loc != null) {
            Resource r = new DefaultResourceLoader().getResource(loc);
            if (!r.exists()) throw new FileNotFoundException("Resource not found: " + loc);
            try (InputStream in = r.getInputStream()) {
                return GoogleCredentials.fromStream(in);
            }
        }
        // 3) GOOGLE_APPLICATION_CREDENTIALS yoki default
        GoogleCredentials cred = GoogleCredentials.getApplicationDefault();
        if (cred == null) {
            throw new IllegalStateException(
                    "Firebase credentials not found. Provide one of: " +
                            "GOOGLE_APPLICATION_CREDENTIALS, firebase.credentials.base64, " +
                            "firebase.credentials.file, or put firebase/serviceAccount.json on classpath.");
        }
        return cred;
    }
}
