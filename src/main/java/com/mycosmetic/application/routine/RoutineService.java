package com.mycosmetic.application.routine;
import com.mycosmetic.application.port.out.LlmPort;

import com.mycosmetic.adapter.in.web.dto.request.RoutineRequest;
import com.mycosmetic.application.routine.RoutineLlmResult;
import com.mycosmetic.adapter.in.web.dto.response.RoutineResponse;
import com.mycosmetic.domain.cosmetic.Cosmetic;
import com.mycosmetic.domain.routine.Routine;
import com.mycosmetic.domain.routine.RoutineCosmetic;
import com.mycosmetic.domain.user.User;
import com.mycosmetic.adapter.out.persistence.CosmeticRepository;
import com.mycosmetic.adapter.out.persistence.RoutineRepository;
import com.mycosmetic.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final CosmeticRepository cosmeticRepository;
    private final UserRepository userRepository;
    private final LlmPort llmClient;

    @Transactional
    public RoutineResponse create(String email, RoutineRequest request) {
        User user = getUser(email);
        List<Cosmetic> cosmetics = cosmeticRepository.findAllByUserId(user.getId());

        if (cosmetics.isEmpty()) {
            throw new IllegalStateException("등록된 화장품이 없어 루틴을 추천할 수 없습니다.");
        }

        RoutineLlmResult llmResult = llmClient.recommendRoutine(user, cosmetics, request.getTimeOfDay());

        Routine routine = Routine.builder()
                .user(user)
                .name(llmResult.getName())
                .timeOfDay(request.getTimeOfDay())
                .description(llmResult.getDescription())
                .build();
        routineRepository.save(routine);

        Map<Long, Cosmetic> cosmeticMap = cosmetics.stream()
                .collect(Collectors.toMap(Cosmetic::getId, Function.identity()));

        for (RoutineLlmResult.StepItem step : llmResult.getSteps()) {
            Cosmetic cosmetic = cosmeticMap.get(step.getCosmeticId());
            if (cosmetic == null) continue; // LLM이 잘못된 ID를 반환한 경우 무시
            routine.getRoutineCosmetics().add(
                    RoutineCosmetic.builder()
                            .routine(routine)
                            .cosmetic(cosmetic)
                            .order(step.getOrder())
                            .build()
            );
        }

        return new RoutineResponse(routine);
    }

    public List<RoutineResponse> findAll(String email) {
        User user = getUser(email);
        return routineRepository.findAllByUserId(user.getId()).stream()
                .map(RoutineResponse::new)
                .toList();
    }

    public void delete(String email, Long routineId) {
        User user = getUser(email);
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("루틴을 찾을 수 없습니다."));
        if (!routine.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        routineRepository.delete(routine);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
