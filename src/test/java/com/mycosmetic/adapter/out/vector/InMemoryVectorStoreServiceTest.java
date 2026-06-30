package com.mycosmetic.adapter.out.vector;
import com.mycosmetic.adapter.out.upstage.UpstageEmbeddingClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InMemoryVectorStoreServiceTest {

    @InjectMocks
    private InMemoryVectorStoreService vectorStoreService;

    @Mock
    private UpstageEmbeddingClient embeddingClient;

    // 단위 벡터: [1,0], [0,1] 은 코사인 유사도 0
    // [1,0] 끼리는 코사인 유사도 1 (동일 방향)
    private static final float[] VEC_X = {1f, 0f};
    private static final float[] VEC_Y = {0f, 1f};
    private static final float[] VEC_DIAG = {1f, 1f}; // [1,0]과 유사도 0.707

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 새 인스턴스가 주입되므로 store는 항상 비어 있음
    }

    // ── addVector / removeVector ────────────────────────────────────

    @Test
    @DisplayName("addVector 후 search하면 해당 ID가 반환된다")
    void addVector_thenSearch_returnsId() {
        given(embeddingClient.embedPassage("토너 이니스프리 정제수")).willReturn(VEC_X);
        given(embeddingClient.embedQuery("query")).willReturn(VEC_X);

        vectorStoreService.addVector(1L, "토너 이니스프리 정제수");
        List<Long> result = vectorStoreService.search("query", 5);

        assertThat(result).containsExactly(1L);
    }

    @Test
    @DisplayName("removeVector 후 search하면 해당 ID가 반환되지 않는다")
    void removeVector_thenSearch_notReturned() {
        given(embeddingClient.embedPassage("토너 이니스프리 정제수")).willReturn(VEC_X);

        vectorStoreService.addVector(1L, "토너 이니스프리 정제수");
        vectorStoreService.removeVector(1L);
        // store가 비어 있으므로 embed 호출 없이 빈 리스트 반환
        List<Long> result = vectorStoreService.search("query", 5);

        assertThat(result).isEmpty();
    }

    // ── search — 코사인 유사도 순서 ─────────────────────────────────

    @Test
    @DisplayName("쿼리와 유사도가 높은 순서로 결과가 정렬된다")
    void search_sortedByCosineSimilarity() {
        // ID 1: [1,0] — 쿼리 [1,0]과 유사도 1.0
        // ID 2: [1,1] — 쿼리 [1,0]과 유사도 0.707
        // ID 3: [0,1] — 쿼리 [1,0]과 유사도 0.0
        given(embeddingClient.embedPassage("A")).willReturn(VEC_X);
        given(embeddingClient.embedPassage("B")).willReturn(VEC_DIAG);
        given(embeddingClient.embedPassage("C")).willReturn(VEC_Y);
        given(embeddingClient.embedQuery("query")).willReturn(VEC_X);

        vectorStoreService.addVector(1L, "A");
        vectorStoreService.addVector(2L, "B");
        vectorStoreService.addVector(3L, "C");

        List<Long> result = vectorStoreService.search("query", 3);

        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("topK가 저장된 벡터 수보다 작으면 topK개만 반환된다")
    void search_topK_limitsResults() {
        given(embeddingClient.embedPassage("A")).willReturn(VEC_X);
        given(embeddingClient.embedPassage("B")).willReturn(VEC_DIAG);
        given(embeddingClient.embedPassage("C")).willReturn(VEC_Y);
        given(embeddingClient.embedQuery("query")).willReturn(VEC_X);

        vectorStoreService.addVector(1L, "A");
        vectorStoreService.addVector(2L, "B");
        vectorStoreService.addVector(3L, "C");

        List<Long> result = vectorStoreService.search("query", 2);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("벡터 스토어가 비어 있으면 빈 리스트를 반환한다")
    void search_emptyStore_returnsEmpty() {
        List<Long> result = vectorStoreService.search("query", 5);

        assertThat(result).isEmpty();
        // 빈 스토어에서는 임베딩 API를 호출하지 않아야 한다
        verify(embeddingClient, org.mockito.Mockito.never()).embedQuery(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("addVector는 동일 ID로 재호출 시 벡터를 덮어쓴다")
    void addVector_sameId_overwritesPrevious() {
        given(embeddingClient.embedPassage("old")).willReturn(VEC_Y);   // [0,1]
        given(embeddingClient.embedPassage("new")).willReturn(VEC_X);   // [1,0]
        given(embeddingClient.embedQuery("query")).willReturn(VEC_X);   // [1,0] 방향 쿼리

        vectorStoreService.addVector(1L, "old");
        vectorStoreService.addVector(1L, "new"); // 덮어쓰기

        List<Long> result = vectorStoreService.search("query", 1);

        // new([1,0])로 덮어써졌으므로 쿼리 [1,0]과 유사도 1.0
        assertThat(result).containsExactly(1L);
    }
}
