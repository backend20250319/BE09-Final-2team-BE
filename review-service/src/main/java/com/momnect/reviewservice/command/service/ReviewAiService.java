package com.momnect.reviewservice.command.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewAiService {

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    @Autowired
    public ReviewAiService(
            WebClient.Builder builder,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${openai.model:gpt-3.5-turbo}") String model,
            @Value("${openai.max_tokens:200}") int maxTokens,
            @Value("${openai.temperature:0.7}") double temperature) {
        this.webClient = builder
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    public String getSummaryAndSentiment(String reviewContent) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", this.model);
        requestBody.put("messages", Collections.singletonList(
                Map.of("role", "user", "content", "다음 리뷰 내용을 긍정적 또는 부정적 감정과 함께 한 문장으로 요약하고, 감정은 마지막에 '감정: [긍정적/부정적]' 형식으로 붙여주세요.\n\n리뷰 내용: " + reviewContent)
        ));
        requestBody.put("max_tokens", this.maxTokens);
        requestBody.put("temperature", this.temperature);

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> messageContent = (Map<String, Object>) firstChoice.get("message");
                if (messageContent != null) {
                    return (String) messageContent.get("content");
                }
            }
            return "Failed to get a valid response from AI.";
        } catch (WebClientResponseException.BadRequest e) {
            System.err.println("OpenAI API call failed with status code " + e.getStatusCode() + " and body: " + e.getResponseBodyAsString());
            return "Failed to summarize review: Bad Request from API.";
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during OpenAI API call: " + e.getMessage());
            return "An unexpected error occurred.";
        }
    }

    // **새로운 메소드 추가: 여러 리뷰를 종합 요약**
    public String getCombinedSummary(List<String> reviewContents) {
        if (reviewContents.isEmpty()) {
            return "아직 리뷰가 없습니다.";
        }

        String combinedContent = String.join("\n", reviewContents);

        String prompt = "다음은 여러 사용자의 리뷰 내용입니다. 이 리뷰들을 종합하여 주요 내용을 100자 이내로 요약해 주세요. 중복되는 내용은 제외하고 핵심만 추출해 주세요.\n\n" + combinedContent;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", this.model);
        requestBody.put("messages", Collections.singletonList(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 200);
        requestBody.put("temperature", this.temperature);

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> messageContent = (Map<String, Object>) firstChoice.get("message");
                if (messageContent != null) {
                    return (String) messageContent.get("content");
                }
            }
            return "Failed to get a valid combined summary from AI.";
        } catch (WebClientResponseException.BadRequest e) {
            System.err.println("OpenAI API call failed: " + e.getResponseBodyAsString());
            return "Failed to summarize reviews: Bad Request from API.";
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during combined summary API call: " + e.getMessage());
            return "An unexpected error occurred.";
        }
    }
}