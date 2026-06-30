package com.mycosmetic.adapter.out.upstage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycosmetic.adapter.in.web.dto.response.OcrParseResult;
import com.mycosmetic.adapter.in.web.dto.response.RoutineLlmResult;
import com.mycosmetic.domain.chat.ChatMessage;
import com.mycosmetic.domain.cosmetic.Cosmetic;
import com.mycosmetic.domain.chat.Role;
import com.mycosmetic.domain.routine.TimeOfDay;
import com.mycosmetic.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
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
              SKIN: 스킨, 토너, 부스터, 미스트, toner, mist
              ESSENCE: 에센스, 세럼, 앰플, essence, serum, ampoule
              CREAM: 크림, 로션, 에멀전, 밀크, 올인원, cream, lotion, emulsion, all-in-one
              SUNSCREEN: 선크림, 선스크린, SPF, sunscreen, sun cream
              CLEANSING: 클렌저, 폼 클렌저, 클렌징 오일/워터, cleanser, cleansing foam
              ETC: 위에 해당하지 않는 경우 (팩, 마스크, 아이크림 등)
              (판단 불가 시 ETC 출력. 절대 다른 문자열 금지)
            - ingredients: 전성분 텍스트를 단 한 글자도 생략하거나 요약하지 말고 OCR 원문 그대로 전부 출력하세요. "...", "(이하 생략)", "(이하 동일)" 같은 표현은 절대 금지. 추출 불가 시 빈 문자열 ""
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

    private static final String ROUTINE_SYSTEM_PROMPT = """
            당신은 피부 전문가입니다. 사용자의 피부 정보와 보유 화장품 목록을 바탕으로 스킨케어 루틴을 추천하세요.
            반드시 JSON 형식으로만 응답하세요.

            규칙:
            - steps에는 주어진 화장품 ID만 사용 (목록에 없는 제품은 포함 금지)
            - order는 1부터 시작하는 사용 순서
            - description은 루틴 전체에 대한 한 문장 설명
            - name은 반드시 시간대에 맞게 작성: AM이면 "아침 루틴", PM이면 "저녁 루틴"

            응답 형식:
            {"name":"아침 루틴","description":"건성 피부를 위한 보습 중심 루틴","steps":[{"cosmeticId":1,"order":1},{"cosmeticId":3,"order":2}]}

            JSON 외의 다른 텍스트는 절대 포함하지 마세요.
            """;

    public RoutineLlmResult recommendRoutine(User user, List<Cosmetic> cosmetics, TimeOfDay timeOfDay) {
        String cosmeticList = cosmetics.stream()
                .map(c -> "ID=%d, 이름=%s, 브랜드=%s, 카테고리=%s".formatted(
                        c.getId(), c.getName(), c.getBrand(), c.getCategory()))
                .collect(Collectors.joining("\n"));

        String userPrompt = """
                피부 타입: %s
                피부 고민: %s
                알레르기 성분: %s
                시간대: %s

                보유 화장품 목록:
                %s
                """.formatted(
                user.getSkinType(),
                user.getSkinConcerns(),
                user.getAllergyIngredients(),
                timeOfDay,
                cosmeticList
        );

        Map<String, Object> requestBody = Map.of(
                "model", llmModel,
                "messages", List.of(
                        Map.of("role", "system", "content", ROUTINE_SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
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
        try {
            return objectMapper.readValue(extractJson(content), RoutineLlmResult.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("루틴 추천 LLM 응답 파싱 실패: " + content, e);
        }
    }

    public String chat(String systemPrompt, List<ChatMessage> history, String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage msg : history) {
            messages.add(Map.of(
                    "role", msg.getRole() == Role.USER ? "user" : "assistant",
                    "content", msg.getContent()
            ));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> requestBody = Map.of("model", llmModel, "messages", messages, "reasoning_effort", "high");

        @SuppressWarnings("unchecked")
        Map<?, ?> response = upstageWebClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return extractContent(response);
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
