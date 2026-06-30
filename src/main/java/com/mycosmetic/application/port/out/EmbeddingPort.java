package com.mycosmetic.application.port.out;

/**
 * 임베딩(Solar Embedding) 호출 outbound 포트. 비대칭 모델(passage/query).
 * 구현체: {@code adapter.out.upstage.UpstageEmbeddingClient}
 */
public interface EmbeddingPort {

    float[] embedPassage(String text);

    float[] embedQuery(String text);
}
