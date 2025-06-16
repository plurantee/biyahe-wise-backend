package com.biyahewise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.biyahewise.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class EstimationService {

    @Value("${OPENAI_API_KEY}")
    private String openAiApiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CommuteResponse estimate(CommuteRequest request) {
        if (request.getMode().equalsIgnoreCase("DRIVE")) {
            return estimateDrive(request);
        } else {
            return estimateCommute(request);
        }
    }

    private CommuteResponse estimateDrive(CommuteRequest request) {
        try {
            String userPrompt = String.format("""
                    You are a car trip estimator.

                    The user is driving from: %s to: %s at: %s.
                    Car: %s

                    Estimate total driving distance, fuel consumption, traffic time, and cost .
                    Return only valid JSON in this format:

                    {
                      "estimatedTimeMinutes": int,
                      "estimatedCostPHP": int,
                      "estimatedLitersUsed": float
                    }

                    DO NOT return any explanation or extra text.
                    """,
                    request.getOrigin(), request.getDestination(), request.getDateTime(), request.getCarDetails());

            String requestBody = buildOpenAIRequest(userPrompt, "You are a highly accurate driving trip estimator.", 500);
            String response = callOpenAI(requestBody);

            JsonNode messageContent = extractMessageContent(response);
            String cleanJson = sanitizeGPTResponse(messageContent.asText());
            JsonNode parsedJson = objectMapper.readTree(cleanJson);

            CommuteResponse res = new CommuteResponse();
            res.setEstimatedLitersUsed(parsedJson.get("estimatedLitersUsed").asDouble());
            res.setOptions(new ArrayList<>());

            Option driveOption = new Option();
            driveOption.setOptionTitle("Drive");
            driveOption.setEstimatedTimeMinutes(parsedJson.get("estimatedTimeMinutes").asDouble());
            driveOption.setEstimatedCostPHP(parsedJson.get("estimatedCostPHP").asDouble());
            driveOption.setSteps(new ArrayList<>());

            res.getOptions().add(driveOption);

            return res;

        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenAI or parse drive response", e);
        }
    }

    private CommuteResponse estimateCommute(CommuteRequest request) {
        try {
            String userPrompt = String.format("""
    You are a commute estimator.

    The user is commuting:
    - From: %s
    - To: %s
    - Time: %s.

    Important Instructions:
    - Search in web the exact address of origin and destination.
    - When referencing the origin and destination, include the full address.
    - Provide multiple commute options as valid JSON.
    - Provide up to 3 options.
    - DO NOT include riding taxi/grab from end to end.
    - Sort by cheapest to most expensive option.
    - Be highly detailed and specific.
    - For each step, include:
      - Street names where riding or walking happens.
      - Exact corner/intersection for pick-up and drop-off.
      - Nearby landmarks when available (mall names, stations, markets, etc).
      - Transfer points if applicable.
    - Use realistic commuting patterns for Metro Manila and neighboring areas.
    - Sort by shortest time covered to longest
    - always add 10 minutes for safety

    Provide your response ONLY as valid JSON, following this format:

    {
      "options": [
        {
          "optionTitle": "Option 1: Mode Combination",
          "estimatedTimeMinutes": int,
          "estimatedCostPHP": int,
          "steps": [
            "Step 1...",
            "Step 2..."
          ]
        }
      ]
    }
    """, request.getOrigin(), request.getDestination(), request.getDateTime());

            String requestBody = buildOpenAIRequest(userPrompt, "You are a highly accurate global commute assistant.", 1200);
            String response = callOpenAI(requestBody);

            JsonNode messageContent = extractMessageContent(response);
            String cleanJson = sanitizeGPTResponse(messageContent.asText());
            JsonNode parsedJson = objectMapper.readTree(cleanJson);

            CommuteResponse res = new CommuteResponse();
            res.setEstimatedLitersUsed(0);  // For commute we don't compute fuel.

            List<Option> optionList = new ArrayList<>();
            parsedJson.get("options").forEach(optionNode -> {
                Option option = new Option();
                option.setOptionTitle(optionNode.get("optionTitle").asText());
                option.setEstimatedTimeMinutes(optionNode.get("estimatedTimeMinutes").asDouble());
                option.setEstimatedCostPHP(optionNode.get("estimatedCostPHP").asDouble());

                List<CommuteStep> steps = new ArrayList<>();
                optionNode.get("steps").forEach(stepNode -> {
                    CommuteStep step = new CommuteStep();
                    step.setDescription(stepNode.asText());
                    steps.add(step);
                });

                option.setSteps(steps);
                optionList.add(option);
            });

            res.setOptions(optionList);
            return res;

        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenAI or parse commute response", e);
        }
    }

    private String buildOpenAIRequest(String userPrompt, String systemPrompt, int maxTokens) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", maxTokens
        );
        return objectMapper.writeValueAsString(requestBody);
    }

    private String callOpenAI(String requestBody) {
        WebClient client = WebClient.builder()
                .baseUrl(OPENAI_URL)
                .defaultHeader("Authorization", "Bearer " + openAiApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        return client.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private JsonNode extractMessageContent(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        return root.at("/choices/0/message/content");
    }

    private String sanitizeGPTResponse(String raw) {
        return raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```", "").trim();
    }

    @PostConstruct
    public void checkKey() {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable not set");
        }
    }
}
