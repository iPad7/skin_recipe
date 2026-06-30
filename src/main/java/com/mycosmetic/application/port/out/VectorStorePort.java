package com.mycosmetic.application.port.out;

import java.util.List;

public interface VectorStorePort {
    void addVector(Long cosmeticId, String text);
    void removeVector(Long cosmeticId);
    List<Long> search(String query, int topK);
}
