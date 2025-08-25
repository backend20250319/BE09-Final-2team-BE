# OpenAiCommonService 사용법

## 개요
`OpenAiCommonService`는 OpenAI API를 사용하기 위한 공용 컴포넌트입니다. 다른 서비스에서도 쉽게 AI 기능을 활용할 수 있습니다.

## 주요 기능

### 1. 기본 OpenAI API 호출
```java
@Autowired
private OpenAiCommonService openAiService;

// 기본 설정으로 API 호출
String response = openAiService.callOpenAiApi("안녕하세요!");

// 커스텀 설정으로 API 호출
String response = openAiService.callOpenAiApi("안녕하세요!", 300, 0.5);
```

### 2. JSON 응답 파싱
```java
// JSON 응답을 Map으로 파싱
Map<String, String> result = openAiService.parseJsonResponse(jsonResponse);
```

### 3. 텍스트 정리
```java
// 공백과 줄바꿈 정리
String cleanedText = openAiService.cleanText("  원본   텍스트\n\n");

// 자연스럽게 텍스트 자르기 (50자)
String truncatedText = openAiService.truncateTextNaturally(originalText, 50);
```

## 사용 예시

### 새로운 서비스에서 사용하기
```java
@Service
public class MyNewService {
    
    @Autowired
    private OpenAiCommonService openAiService;
    
    public String generateSummary(String content) {
        String prompt = "다음 내용을 요약해주세요: " + content;
        String aiResponse = openAiService.callOpenAiApi(prompt);
        return openAiService.cleanText(aiResponse);
    }
    
    public Map<String, String> analyzeSentiment(String text) {
        String prompt = "다음 텍스트의 감정을 분석해주세요: " + text;
        String aiResponse = openAiService.callOpenAiApi(prompt);
        return openAiService.parseJsonResponse(aiResponse);
    }
}
```

### 설정 정보 확인
```java
String model = openAiService.getModel();           // "gpt-3.5-turbo"
int maxTokens = openAiService.getMaxTokens();     // 200
double temperature = openAiService.getTemperature(); // 0.7
```

## 장점
1. **재사용성**: 여러 서비스에서 동일한 OpenAI 기능 사용 가능
2. **일관성**: 동일한 설정과 에러 처리로 일관된 결과
3. **유지보수성**: OpenAI 관련 로직을 한 곳에서 관리
4. **확장성**: 새로운 AI 기능을 쉽게 추가 가능

## 주의사항
- `spring.ai.openai.api-key` 설정이 필요합니다
- WebClient 의존성이 필요합니다
- ObjectMapper 의존성이 필요합니다
