package com.mycosmetic.service;

import java.util.List;

public interface VectorStoreService {
    void addVector(Long cosmeticId, String text);
    void removeVector(Long cosmeticId);
    List<Long> search(String query, int topK);
}
