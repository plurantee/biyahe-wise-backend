package com.biyahewise.controller;

import com.biyahewise.model.CommuteRequest;
import com.biyahewise.model.CommuteResponse;
import com.biyahewise.service.EstimationService;
import com.biyahewise.service.FirebaseTokenService;
import com.biyahewise.service.QueryHistoryService;
import com.biyahewise.service.QuotaService;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/estimate")
@CrossOrigin(origins = "*")
@Slf4j
public class EstimateController {

    @Autowired
    private EstimationService estimationService;

    @Autowired
    private FirebaseTokenService tokenService;

    @Autowired
    private QueryHistoryService queryHistoryService;

    @Autowired
    private QuotaService quotaService;

    @PostMapping
    public ResponseEntity<?> estimate(
            HttpServletRequest request,
            @RequestBody CommuteRequest commuteRequest) {

        try {
            log.info("Request entry: origin: {}, destination: {}", commuteRequest.getOrigin(), commuteRequest.getDestination());
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
            }

            String idToken = authHeader.substring(7);
            FirebaseToken decodedToken = tokenService.verifyToken(idToken);
            String uid = decodedToken.getUid();
            System.out.println("Authenticated Firebase UID: " + uid);

            if (!quotaService.isRequestAllowed(uid)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Daily limit reached.");
            }
            CommuteResponse response = estimationService.estimate(commuteRequest);

            quotaService.incrementUsage(uid);
            queryHistoryService.saveQuery(uid, commuteRequest, response);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing token");
            }

            String idToken = authHeader.substring(7);
            FirebaseToken decodedToken = tokenService.verifyToken(idToken);
            String uid = decodedToken.getUid();

            List<QueryHistoryService.HistoryEntry> history = queryHistoryService.getHistory(uid);
            return ResponseEntity.ok(history);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving history");
        }
    }
}
