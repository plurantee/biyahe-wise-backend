package com.biyahewise.service;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class QuotaService {

    private static final int DAILY_LIMIT = 3;

    public boolean isRequestAllowed(String uid) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference ref = db.collection("user_quotas").document(uid);
        DocumentSnapshot snapshot = ref.get().get();

        LocalDate today = LocalDate.now();
        String todayStr = today.toString();

        if (!snapshot.exists()) {
            // New user — allow request
            return true;
        }

        String storedDate = snapshot.getString("requestDate");
        Long requestCount = snapshot.getLong("requestCount");

        if (!todayStr.equals(storedDate)) {
            // New day — reset quota
            return true;
        }

        return requestCount == null || requestCount < DAILY_LIMIT;
    }

    public void incrementUsage(String uid) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference ref = db.collection("user_quotas").document(uid);
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref).get();

            long count = 0;
            if (snapshot.exists()) {
                String storedDate = snapshot.getString("requestDate");
                if (todayStr.equals(storedDate)) {
                    count = snapshot.getLong("requestCount") != null ? snapshot.getLong("requestCount") : 0;
                }
            }

            transaction.set(ref, Map.of(
                    "requestDate", todayStr,
                    "requestCount", count + 1
            ));
            return null;
        }).get();
    }
}
