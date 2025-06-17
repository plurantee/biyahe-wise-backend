package com.biyahewise.service;


import com.biyahewise.model.CommuteRequest;
import com.biyahewise.model.CommuteResponse;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class QueryHistoryService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AllArgsConstructor
    public static class HistoryEntry {
        public String origin;
        public String destination;
        public String response;
    }

    public void saveQuery(String uid, CommuteRequest request, CommuteResponse response) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        Map<String, Object> data = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "request", objectMapper.writeValueAsString(request),
                "response", objectMapper.writeValueAsString(response)
        );

        db.collection("query_history")
                .document(uid)
                .collection("logs")
                .add(data);
    }

    public List<HistoryEntry> getHistory(String uid) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        CollectionReference logs = db.collection("query_history").document(uid).collection("logs");
        ApiFuture<QuerySnapshot> future = logs.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<HistoryEntry> results = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
            try {
                String requestJson = doc.getString("request");
                String responseJson = doc.getString("response");
                Map<String, Object> requestMap = objectMapper.readValue(requestJson, Map.class);

                String origin = requestMap.getOrDefault("origin", "").toString();
                String destination = requestMap.getOrDefault("destination", "").toString();

                results.add(new HistoryEntry(origin, destination, responseJson));
            } catch (Exception e) {
                e.printStackTrace(); // handle malformed data gracefully
            }
        }

        return results;
    }
}
