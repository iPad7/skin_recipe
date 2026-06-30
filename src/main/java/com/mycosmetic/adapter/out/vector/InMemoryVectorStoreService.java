package com.mycosmetic.adapter.out.vector;
import com.mycosmetic.application.port.out.VectorStoreService;
import com.mycosmetic.adapter.out.upstage.UpstageEmbeddingClient;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Service
@RequiredArgsConstructor
public class InMemoryVectorStoreService implements VectorStoreService {

    private final UpstageEmbeddingClient embeddingClient;
    private final ConcurrentHashMap<Long, float[]> store = new ConcurrentHashMap<>();

    @Override
    public void addVector(Long cosmeticId, String text) {
        float[] vector = embeddingClient.embedPassage(text);
        store.put(cosmeticId, vector);
    }

    @Override
    public void removeVector(Long cosmeticId) {
        store.remove(cosmeticId);
    }

    @Override
    public List<Long> search(String query, int topK) {
        if (store.isEmpty()) {
            return List.of();
        }
        float[] queryVector = embeddingClient.embedQuery(query);
        return store.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), cosineSimilarity(queryVector, e.getValue())))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
