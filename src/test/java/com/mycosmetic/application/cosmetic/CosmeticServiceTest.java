package com.mycosmetic.application.cosmetic;

import com.mycosmetic.application.port.out.CosmeticRepository;
import com.mycosmetic.application.port.out.RoutineCosmeticRepository;
import com.mycosmetic.application.port.out.UserRepository;
import com.mycosmetic.application.port.out.VectorStorePort;
import com.mycosmetic.domain.cosmetic.Cosmetic;
import com.mycosmetic.domain.cosmetic.CosmeticCategory;
import com.mycosmetic.domain.user.SkinType;
import com.mycosmetic.domain.user.User;
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

    @Mock
    private VectorStorePort vectorStoreService;

    @Mock
    private RoutineCosmeticRepository routineCosmeticRepository;

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

        List<CosmeticResult> result = cosmeticService.findAll("user@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("토너");
    }

    // ── 저장 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("화장품을 정상 저장한다")
    void save_success() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(cosmeticRepository.save(any(Cosmetic.class))).willReturn(cosmetic);

        CosmeticResult response = cosmeticService.save("user@example.com", saveCommand("토너", CosmeticCategory.SKIN));

        assertThat(response.name()).isEqualTo("토너");
        verify(cosmeticRepository).save(any(Cosmetic.class));
    }

    // ── 수정 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("내 화장품을 수정한다")
    void update_success() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(cosmeticRepository.findById(10L)).willReturn(Optional.of(cosmetic));

        CosmeticResult response = cosmeticService.update("user@example.com", 10L,
                updateCommand("수분크림", CosmeticCategory.CREAM));

        assertThat(response.name()).isEqualTo("수분크림");
    }

    @Test
    @DisplayName("다른 사람의 화장품을 수정하면 예외가 발생한다")
    void update_notOwner() {
        given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
        given(cosmeticRepository.findById(10L)).willReturn(Optional.of(cosmetic));  // user 소유

        assertThatThrownBy(() -> cosmeticService.update("other@example.com", 10L,
                updateCommand("토너", CosmeticCategory.SKIN)))
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

    private SaveCosmeticCommand saveCommand(String name, CosmeticCategory category) {
        return new SaveCosmeticCommand(name, null, category, null, null);
    }

    private UpdateCosmeticCommand updateCommand(String name, CosmeticCategory category) {
        return new UpdateCosmeticCommand(name, null, category, null);
    }

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
}
