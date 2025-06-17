package com.biyahewise.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() throws Exception {
        String firebaseBase64 = System.getenv("FIREBASE_CONFIG_BASE64");
        if (firebaseBase64 == null) {
            throw new IllegalStateException("Missing FIREBASE_CONFIG_BASE64 env var");
        }

        byte[] decodedBytes = Base64.getDecoder().decode(firebaseBase64);
        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decodedBytes));

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
