package com.momnect.openaiservice.common;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * OpenAI API를 사용하기 위한 공용 컴포넌트 서비스
 * 다른 서비스에서도 쉽게 사용할 수 있도록 설계
 */
@Service
public class OpenAiCommonService {

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenAiCommonService(
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
     * OpenAI API를 호출하여 응답을 받는 공용 메서드
     * 
     * @param prompt 사용자 프롬프트
     * @param maxTokens 최대 토큰 수 (기본값 사용하려면 null)
     * @param temperature 온도 설정 (기본값 사용하려면 null)
     * @return AI 응답 텍스트
     */
    public String callOpenAiApi(String prompt, Integer maxTokens, Double temperature) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", this.model);
        requestBody.put("messages", Collections.singletonList(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", maxTokens != null ? maxTokens : this.maxTokens);
        requestBody.put("temperature", temperature != null ? temperature : this.temperature);

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
            return "AI 응답 생성에 실패했습니다.";
        } catch (WebClientResponseException.BadRequest e) {
            System.err.println("OpenAI API call failed: " + e.getResponseBodyAsString());
            return "AI 응답 생성 중 오류가 발생했습니다.";
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during OpenAI API call: " + e.getMessage());
            return "AI 응답 생성 중 예상치 못한 오류가 발생했습니다.";
        }
    }

    /**
     * 기본 설정으로 OpenAI API를 호출하는 메서드
     * 
     * @param prompt 사용자 프롬프트
     * @return AI 응답 텍스트
     */
    public String callOpenAiApi(String prompt) {
        return callOpenAiApi(prompt, null, null);
    }

    /**
     * JSON 응답을 파싱하여 Map으로 반환하는 메서드
     * 
     * @param jsonResponse JSON 문자열
     * @return 파싱된 Map 객체
     */
    public Map<String, String> parseJsonResponse(String jsonResponse) {
        try {
            return objectMapper.readValue(jsonResponse, Map.class);
        } catch (Exception e) {
            System.err.println("JSON 파싱 실패: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 텍스트를 정리하는 공용 메서드 (공백 제거, 줄바꿈 제거)
     * 
     * @param text 원본 텍스트
     * @return 정리된 텍스트
     */
    public String cleanText(String text) {
        if (text == null) return null;
        return text.trim().replaceAll("\\s+", " ").replaceAll("\n", " ");
    }

    /**
     * 텍스트를 지정된 길이로 자연스럽게 자르는 메서드
     * 
     * @param text 원본 텍스트
     * @param maxLength 최대 길이
     * @return 잘린 텍스트
     */
    public String truncateTextNaturally(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        // 마지막 완전한 문장을 찾아서 자르기
        int lastPeriod = text.lastIndexOf('.');
        int lastExclamation = text.lastIndexOf('!');
        int lastQuestion = text.lastIndexOf('?');
        
        int lastSentenceEnd = Math.max(Math.max(lastPeriod, lastExclamation), lastQuestion);
        
        if (lastSentenceEnd > 0 && lastSentenceEnd <= maxLength) {
            return text.substring(0, lastSentenceEnd + 1);
        } else {
            // 문장 끝을 찾을 수 없으면 maxLength에서 자르되 단어 중간에서 자르지 않기
            int cutPoint = text.lastIndexOf(' ', maxLength);
            if (cutPoint > 0) {
                return text.substring(0, cutPoint) + "...";
            } else {
                return text.substring(0, maxLength) + "...";
            }
        }
    }

    /**
     * 현재 설정된 모델 정보를 반환
     */
    public String getModel() {
        return this.model;
    }

    /**
     * 현재 설정된 최대 토큰 수를 반환
     */
    public int getMaxTokens() {
        return this.maxTokens;
    }

    /**
     * 현재 설정된 온도를 반환
     */
    public double getTemperature() {
        return this.temperature;
    }
}
