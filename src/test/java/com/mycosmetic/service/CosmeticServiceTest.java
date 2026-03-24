package com.mycosmetic.service;

import com.mycosmetic.dto.request.CosmeticRequest;
import com.mycosmetic.dto.response.CosmeticResponse;
import com.mycosmetic.entity.Cosmetic;
import com.mycosmetic.entity.CosmeticCategory;
import com.mycosmetic.entity.SkinType;
import com.mycosmetic.entity.User;
import com.mycosmetic.repository.CosmeticRepository;
import com.mycosmetic.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CosmeticServiceTest {

    @InjectMocks
    private CosmeticService cosmeticService;

    @Mock
    private CosmeticRepository cosmeticRepository;

    @Mock
    private UserRepository userRepository;

    private User user;
    private User otherUser;
    private Cosmetic cosmetic;

    @BeforeEach
    void setUp() {
        user = makeUser(1L, "user@example.com");
        otherUser = makeUser(2L, "other@example.com");
        cosmetic = makeCosmetic(10L, user);
    }

    // ── 조회 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("내 화장품 목록을 조회한다")
    void findAll() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(cosmeticRepository.findAllByUserId(1L)).willReturn(List.of(cosmetic));

        List<CosmeticResponse> result = cosmeticService.findAll("user@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("토너");
    }

    // ── 저장 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("화장품을 정상 저장한다")
    void save_success() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(cosmeticRepository.save(any(Cosmetic.class))).willReturn(cosmetic);

        CosmeticResponse response = cosmeticService.save("user@example.com", makeRequest());

        assertThat(response.getName()).isEqualTo("토너");
        verify(cosmeticRepository).save(any(Cosmetic.class));
    }

    // ── 수정 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("내 화장품을 수정한다")
    void update_success() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(cosmeticRepository.findById(10L)).willReturn(Optional.of(cosmetic));

        CosmeticRequest updateRequest = makeRequest("수분크림", CosmeticCategory.CREAM);
        CosmeticResponse response = cosmeticService.update("user@example.com", 10L, updateRequest);

        assertThat(response.getName()).isEqualTo("수분크림");
    }

    @Test
    @DisplayName("다른 사람의 화장품을 수정하면 예외가 발생한다")
    void update_notOwner() {
        given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
        given(cosmeticRepository.findById(10L)).willReturn(Optional.of(cosmetic));  // user 소유

        assertThatThrownBy(() -> cosmeticService.update("other@example.com", 10L, makeRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("접근 권한");
    }

    // ── 삭제 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("내 화장품을 삭제한다")
    void delete_success() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(cosmeticRepository.findById(10L)).willReturn(Optional.of(cosmetic));

        cosmeticService.delete("user@example.com", 10L);

        verify(cosmeticRepository).delete(cosmetic);
    }

    @Test
    @DisplayName("다른 사람의 화장품을 삭제하면 예외가 발생한다")
    void delete_notOwner() {
        given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
        given(cosmeticRepository.findById(10L)).willReturn(Optional.of(cosmetic));

        assertThatThrownBy(() -> cosmeticService.delete("other@example.com", 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("접근 권한");

        verify(cosmeticRepository, never()).delete(any());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────

    private User makeUser(Long id, String email) {
        try {
            User u = User.builder()
                    .email(email).password("encoded").nickname("테스터").skinType(SkinType.NORMAL)
                    .build();
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(u, id);
            return u;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Cosmetic makeCosmetic(Long id, User owner) {
        try {
            Cosmetic c = Cosmetic.builder()
                    .user(owner).name("토너").brand("이니스프리")
                    .category(CosmeticCategory.SKIN).ingredients("정제수, 부틸렌글라이콜")
                    .build();
            var field = Cosmetic.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(c, id);
            return c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CosmeticRequest makeRequest() {
        return makeRequest("토너", CosmeticCategory.SKIN);
    }

    private CosmeticRequest makeRequest(String name, CosmeticCategory category) {
        try {
            CosmeticRequest req = new CosmeticRequest();
            setField(req, "name", name);
            setField(req, "category", category);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
