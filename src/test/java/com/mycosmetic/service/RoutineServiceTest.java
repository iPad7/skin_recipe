package com.mycosmetic.service;

import com.mycosmetic.dto.request.RoutineRequest;
import com.mycosmetic.dto.response.RoutineLlmResult;
import com.mycosmetic.dto.response.RoutineResponse;
import com.mycosmetic.entity.*;
import com.mycosmetic.repository.CosmeticRepository;
import com.mycosmetic.repository.RoutineRepository;
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
class RoutineServiceTest {

    @InjectMocks
    private RoutineService routineService;

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private CosmeticRepository cosmeticRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UpstageLlmClient llmClient;

    private User user;
    private User otherUser;
    private Cosmetic cosmetic;

    @BeforeEach
    void setUp() {
        user = makeUser(1L, "user@example.com");
        otherUser = makeUser(2L, "other@example.com");
        cosmetic = makeCosmetic(10L, user);
    }

    // ── 루틴 생성 ───────────────────────────────────────────────────

    @Test
    @DisplayName("보유 화장품이 있으면 LLM 추천으로 루틴이 생성된다")
    void create_success() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(cosmeticRepository.findAllByUserId(1L)).willReturn(List.of(cosmetic));
        given(llmClient.recommendRoutine(any(), any(), any()))
                .willReturn(makeLlmResult("아침 루틴", "보습 루틴", 10L, 1));

        RoutineResponse response = routineService.create("user@example.com", makeRequest(TimeOfDay.AM));

        assertThat(response.getName()).isEqualTo("아침 루틴");
        assertThat(response.getTimeOfDay()).isEqualTo(TimeOfDay.AM);
        assertThat(response.getDescription()).isEqualTo("보습 루틴");
        assertThat(response.getSteps()).hasSize(1);
        assertThat(response.getSteps().get(0).getCosmeticId()).isEqualTo(10L);
        verify(routineRepository).save(any(Routine.class));
    }

    @Test
    @DisplayName("보유 화장품이 없으면 예외가 발생한다")
    void create_emptyCosmetics_throwsException() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(cosmeticRepository.findAllByUserId(1L)).willReturn(List.of());

        assertThatThrownBy(() -> routineService.create("user@example.com", makeRequest(TimeOfDay.AM)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("등록된 화장품이 없어");

        verify(llmClient, never()).recommendRoutine(any(), any(), any());
        verify(routineRepository, never()).save(any());
    }

    @Test
    @DisplayName("LLM이 존재하지 않는 화장품 ID를 반환하면 해당 step은 무시된다")
    void create_llmReturnsInvalidId_stepSkipped() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(cosmeticRepository.findAllByUserId(1L)).willReturn(List.of(cosmetic));
        // cosmeticId=99 는 목록에 없는 ID
        given(llmClient.recommendRoutine(any(), any(), any()))
                .willReturn(makeLlmResult("루틴", "설명", 99L, 1));

        RoutineResponse response = routineService.create("user@example.com", makeRequest(TimeOfDay.AM));

        assertThat(response.getSteps()).isEmpty();
    }

    // ── 루틴 조회 ───────────────────────────────────────────────────

    @Test
    @DisplayName("내 루틴 목록을 조회한다")
    void findAll_returnsUserRoutines() {
        Routine routine = makeRoutine(1L, user, "아침 루틴", TimeOfDay.AM);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(routineRepository.findAllByUserId(1L)).willReturn(List.of(routine));

        List<RoutineResponse> result = routineService.findAll("user@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("아침 루틴");
    }

    // ── 루틴 삭제 ───────────────────────────────────────────────────

    @Test
    @DisplayName("내 루틴을 삭제한다")
    void delete_success() {
        Routine routine = makeRoutine(1L, user, "아침 루틴", TimeOfDay.AM);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(routineRepository.findById(1L)).willReturn(Optional.of(routine));

        routineService.delete("user@example.com", 1L);

        verify(routineRepository).delete(routine);
    }

    @Test
    @DisplayName("존재하지 않는 루틴 삭제 시 예외가 발생한다")
    void delete_notFound_throwsException() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(routineRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.delete("user@example.com", 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("루틴을 찾을 수 없습니다");

        verify(routineRepository, never()).delete(any());
    }

    @Test
    @DisplayName("다른 사람의 루틴을 삭제하면 예외가 발생한다")
    void delete_notOwner_throwsException() {
        Routine routine = makeRoutine(1L, user, "아침 루틴", TimeOfDay.AM);
        given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
        given(routineRepository.findById(1L)).willReturn(Optional.of(routine));

        assertThatThrownBy(() -> routineService.delete("other@example.com", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("접근 권한");

        verify(routineRepository, never()).delete(any());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────

    private User makeUser(Long id, String email) {
        try {
            User u = User.builder()
                    .email(email).password("encoded").nickname("테스터")
                    .skinType(SkinType.NORMAL).build();
            setField(u, "id", id);
            return u;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Cosmetic makeCosmetic(Long id, User owner) {
        try {
            Cosmetic c = Cosmetic.builder()
                    .user(owner).name("토너").brand("이니스프리")
                    .category(CosmeticCategory.SKIN).ingredients("정제수")
                    .build();
            setField(c, "id", id);
            return c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Routine makeRoutine(Long id, User owner, String name, TimeOfDay timeOfDay) {
        try {
            Routine r = Routine.builder()
                    .user(owner).name(name).timeOfDay(timeOfDay).description("설명")
                    .build();
            setField(r, "id", id);
            return r;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RoutineRequest makeRequest(TimeOfDay timeOfDay) {
        try {
            RoutineRequest req = new RoutineRequest();
            setField(req, "timeOfDay", timeOfDay);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RoutineLlmResult makeLlmResult(String name, String description, Long cosmeticId, int order) {
        try {
            RoutineLlmResult result = new RoutineLlmResult();
            setField(result, "name", name);
            setField(result, "description", description);

            RoutineLlmResult.StepItem step = new RoutineLlmResult.StepItem();
            setField(step, "cosmeticId", cosmeticId);
            setField(step, "order", order);

            setField(result, "steps", List.of(step));
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        // 선언된 클래스부터 상위 클래스까지 탐색
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                var field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
