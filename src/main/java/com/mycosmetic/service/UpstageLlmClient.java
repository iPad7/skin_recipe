package com.mycosmetic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycosmetic.dto.response.OcrParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UpstageLlmClient {

    private final WebClient upstageWebClient;
    private final ObjectMapper objectMapper;

    @Value("${upstage.model.llm}")
    private String llmModel;

    private static final String OCR_PARSE_SYSTEM_PROMPT = """
            당신은 화장품 성분표 파싱 전문가입니다.
            주어진 OCR 텍스트에서 다음 정보를 추출하여 반드시 JSON 형식으로만 응답하세요.

            각 필드 규칙:
            - name: 제품명 (추출 불가 시 빈 문자열 "")
            - brand: 브랜드명 (추출 불가 시 빈 문자열 "")
            - category: 반드시 다음 값 중 정확히 하나만 출력 → SKIN, ESSENCE, CREAM, SUNSCREEN, CLEANSING, ETC
              (판단 불가 시 ETC 출력. 절대 다른 문자열 금지)
            - ingredients: 전성분 텍스트 원문 그대로 (추출 불가 시 빈 문자열 "")
            - confidence: high 또는 low 중 하나
              (high: 제품명/브랜드/전성분 모두 명확히 추출된 경우, low: 하나라도 불확실하거나 누락된 경우)

            응답 예시:
            {"name":"퓨어클린 클렌징 폼","brand":"클린뷰티","category":"CLEANSING","ingredients":"정제수, ...","confidence":"high"}

            JSON 외의 다른 텍스트는 절대 포함하지 마세요.
            """;

    public OcrParseResult parseCosmetic(String ocrText) {
        Map<String, Object> requestBody = Map.of(
                "model", llmModel,
                "messages", List.of(
                        Map.of("role", "system", "content", OCR_PARSE_SYSTEM_PROMPT),
                        Map.of("role", "user", "content", ocrText)
                )
        );

        Map<?, ?> response = upstageWebClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String content = extractContent(response);
        return parseJson(content);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private OcrParseResult parseJson(String content) {
        // LLM이 ```json ... ``` 블록으로 감쌀 경우 대비
        String json = extractJson(content);
        try {
            return objectMapper.readValue(json, OcrParseResult.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("LLM 응답 파싱 실패: " + content, e);
        }
    }

    private String extractJson(String content) {
        Pattern pattern = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start != -1 && end != -1) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}
