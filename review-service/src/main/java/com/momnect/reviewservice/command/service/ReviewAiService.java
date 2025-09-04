package com.momnect.reviewservice.command.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momnect.reviewservice.command.dto.ReviewSummaryResponse;
import com.momnect.reviewservice.command.entity.ReviewSummary;
import com.momnect.reviewservice.command.repository.ReviewSummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewAiService {

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private ReviewSummaryRepository reviewSummaryRepository;

    @Autowired
    public ReviewAiService(
            WebClient.Builder builder,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${openai.model:gpt-3.5-turbo}") String model,
            @Value("${openai.max_tokens:200}") int maxTokens,
            @Value("${openai.temperature:0.7}") double temperature,
            ObjectMapper objectMapper) {
        this.webClient = builder
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.objectMapper = objectMapper;
    }

    /**
     * AI를 사용하여 여러 리뷰의 내용을 종합적으로 요약합니다.
     */
    public String getCombinedSummary(List<String> contents, String sentiment) {
        String prompt = "다음 " + sentiment + " 리뷰들을 읽고 중고 거래 사이트에 맞게 요약해주세요.\n\n" +
                "중요한 규칙:\n" +
                "1. " + sentiment + " 감정에 맞는 내용만 요약해야 합니다\n" +
                "2. " + (sentiment.equals("긍정적") ? "긍정적인 평가와 만족스러운 점만 포함" : "부정적인 평가와 불만족스러운 점만 포함") + "\n" +
                "3. 반대 감정의 내용은 절대 포함하지 마세요\n" +
                "4. 중복되는 내용은 통합하여 간결하게 표현\n" +
                "5. 요약글은 완전한 문장으로 끝나야 함\n" +
                "6. 50자 이내로 작성하되 자연스럽게 끊기지 않게\n" +
                "7. 중고 거래 관련 키워드 사용: 거래 품질, 신뢰성, 서비스, 배송, 가격, 상품 상태 등\n\n" +
                "리뷰 내용:\n" + String.join("\n", contents) + "\n\n" +
                "요약:";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", this.model);
        requestBody.put("messages", Collections.singletonList(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 200);
        requestBody.put("temperature", 0.7);

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
                    String summary = (String) messageContent.get("content");
                    // 요약글 정리 (불필요한 공백 제거, 줄바꿈 제거)
                    if (summary != null) {
                        summary = summary.trim().replaceAll("\\s+", " ").replaceAll("\n", " ");
                        // 50자 제한 (자연스럽게)
                        if (summary.length() > 50) {
                            // 마지막 완전한 문장을 찾아서 자르기
                            int lastPeriod = summary.lastIndexOf('.');
                            int lastExclamation = summary.lastIndexOf('!');
                            int lastQuestion = summary.lastIndexOf('?');
                            
                            int lastSentenceEnd = Math.max(Math.max(lastPeriod, lastExclamation), lastQuestion);
                            
                            if (lastSentenceEnd > 0 && lastSentenceEnd <= 50) {
                                summary = summary.substring(0, lastSentenceEnd + 1);
                            } else {
                                // 문장 끝을 찾을 수 없으면 50자에서 자르되 단어 중간에서 자르지 않기
                                int cutPoint = summary.lastIndexOf(' ', 50);
                                if (cutPoint > 0) {
                                    summary = summary.substring(0, cutPoint) + "...";
                                } else {
                                    summary = summary.substring(0, 50) + "...";
                                }
                            }
                        }
                    }
                    return summary;
                }
            }
            return "AI 요약 생성에 실패했습니다.";
        } catch (WebClientResponseException.BadRequest e) {
            System.err.println("OpenAI API call failed: " + e.getResponseBodyAsString());
            return "AI 요약 생성 중 오류가 발생했습니다.";
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during combined summary API call: " + e.getMessage());
            return "AI 요약 생성 중 예상치 못한 오류가 발생했습니다.";
        }
    }

    /**
     * AI를 사용하여 리뷰의 요약과 감정을 동시에 분석합니다.
     */
    public Map<String, String> getReviewAnalysis(String content, float rating, Boolean kind, Boolean promise, Boolean satisfaction) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 중고 거래 리뷰를 50자 이내로 요약하고, 긍정적 또는 부정적 감정을 분석해줘. JSON 형식으로 결과를 반환해줘.\n");
        prompt.append("--- 리뷰 정보 ---\n");
        prompt.append("내용: ").append(content).append("\n");
        prompt.append("별점: ").append(rating).append("점\n");
        if (kind != null) prompt.append("친절해요: ").append(kind ? "네" : "아니오").append("\n");
        if (promise != null) prompt.append("약속을 잘 지켜요: ").append(promise ? "네" : "아니오").append("\n");
        if (satisfaction != null) prompt.append("만족스러워요: ").append(satisfaction ? "네" : "아니오").append("\n");
        prompt.append("--- 분석 기준 ---\n");
        prompt.append("중고 거래 품질, 판매자/구매자 신뢰성, 서비스 만족도, 약속 준수, 상품 상태 등을 종합적으로 고려\n");
        prompt.append("--- 응답 형식 ---\n");
        prompt.append("{\"summary\": \"리뷰 요약 내용\", \"sentiment\": \"긍정적\" 또는 \"부정적\"}");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", this.model);
        requestBody.put("messages", Collections.singletonList(
                Map.of("role", "user", "content", prompt.toString())
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
                    String jsonResponse = (String) messageContent.get("content");
                    Map<String, String> result = objectMapper.readValue(jsonResponse, Map.class);

                    if (result.containsKey("summary") && result.get("summary") != null) {
                        String summary = result.get("summary").trim();
                        if (summary.length() > 50) {
                            result.put("summary", summary.substring(0, 50));
                        }
                    }
                    return result;
                }
            }
        } catch (WebClientResponseException.BadRequest e) {
            System.err.println("OpenAI API call failed: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during review analysis API call: " + e.getMessage());
        }
        return Collections.emptyMap();
    }

    /**
     * 긍정적/부정적 리뷰의 요약글을 AI로 생성하고 DB에 저장합니다.
     */
    public String generateAndSaveSentimentSummary(String sentiment, List<String> contents, Long reviewCount) {
        if (contents.isEmpty()) {
            return sentiment + " 리뷰가 존재하지 않습니다.";
        }

        // AI로 요약글 생성
        String summaryContent = getCombinedSummary(contents, sentiment);

        // DB에 저장 또는 업데이트
        ReviewSummary reviewSummary = reviewSummaryRepository.findBySentiment(sentiment)
                .orElse(ReviewSummary.builder()
                        .sentiment(sentiment)
                        .summaryContent(summaryContent)
                        .reviewCount(reviewCount)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

        reviewSummary.setSummaryContent(summaryContent);
        reviewSummary.setReviewCount(reviewCount);
        reviewSummary.setUpdatedAt(LocalDateTime.now());

        reviewSummaryRepository.save(reviewSummary);

        return summaryContent;
    }

    /**
     * 저장된 요약글을 조회합니다.
     */
    public String getStoredSentimentSummary(String sentiment) {
        return reviewSummaryRepository.findBySentiment(sentiment)
                .map(ReviewSummary::getSummaryContent)
                .orElse(sentiment + " 리뷰가 존재하지 않습니다.");
    }

    /**
     * 모든 저장된 요약글 정보를 조회합니다.
     */
    public List<ReviewSummaryResponse> getAllStoredSummaries() {
        return reviewSummaryRepository.findAll().stream()
                .map(summary -> new ReviewSummaryResponse(
                        summary.getSentiment(),
                        summary.getSummaryContent(),
                        summary.getReviewCount(),
                        summary.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 모든 감정별 요약글을 새로 생성하고 저장합니다.
     */
    public void regenerateAllSentimentSummaries() {
        // 긍정적 리뷰 요약글 재생성
        List<String> positiveContents = getPositiveReviewContents();
        if (!positiveContents.isEmpty()) {
            generateAndSaveSentimentSummary("긍정적", positiveContents, (long) positiveContents.size());
        }

        // 부정적 리뷰 요약글 재생성
        List<String> negativeContents = getNegativeReviewContents();
        if (!negativeContents.isEmpty()) {
            generateAndSaveSentimentSummary("부정적", negativeContents, (long) negativeContents.size());
        }
    }

    /**
     * 긍정적 리뷰 내용들을 가져옵니다.
     */
    private List<String> getPositiveReviewContents() {
        // 이 메서드는 ReviewService에서 호출되어 실제 긍정적 리뷰 내용을 가져와야 합니다.
        // 현재는 빈 리스트를 반환하고, ReviewService에서 실제 구현을 제공합니다.
        return Collections.emptyList();
    }

    /**
     * 부정적 리뷰 내용들을 가져옵니다.
     */
    private List<String> getNegativeReviewContents() {
        // 이 메서드는 ReviewService에서 호출되어 실제 부정적 리뷰 내용을 가져와야 합니다.
        // 현재는 빈 리스트를 반환하고, ReviewService에서 실제 구현을 제공합니다.
        return Collections.emptyList();
    }
}