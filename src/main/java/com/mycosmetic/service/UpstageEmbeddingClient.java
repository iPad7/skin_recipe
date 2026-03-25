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

    @Value("${upstage.model.embedding}")
    private String embeddingModel;

    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "input", text
        );

        Map<?, ?> response = upstageWebClient.post()
                .uri("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        List<Double> embedding = (List<Double>) data.get(0).get("embedding");

        float[] result = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            result[i] = embedding.get(i).floatValue();
        }
        return result;
    }
}
