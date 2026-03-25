package com.mycosmetic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UpstageEmbeddingClient {

    private final WebClient upstageWebClient;

    @Value("${upstage.model.embedding-passage}")
    private String passageModel;

    @Value("${upstage.model.embedding-query}")
    private String queryModel;

    public float[] embedPassage(String text) {
        return embed(passageModel, text);
    }

    public float[] embedQuery(String text) {
        return embed(queryModel, text);
    }

    @SuppressWarnings("unchecked")
    private float[] embed(String model, String text) {
        Map<String, Object> requestBody = Map.of("model", model, "input", text);

        Map<?, ?> response = upstageWebClient.post()
                .uri("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<?> embedding = (List<?>) data.get(0).get("embedding");

        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = ((Number) embedding.get(i)).floatValue();
        }
        return result;
    }
}
