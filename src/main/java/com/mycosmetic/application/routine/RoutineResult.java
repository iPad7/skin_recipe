package com.mycosmetic.application.routine;

import com.mycosmetic.domain.routine.Routine;
import com.mycosmetic.domain.routine.TimeOfDay;

import java.time.LocalDateTime;
import java.util.List;

public record RoutineResult(
        Long id,
        String name,
        TimeOfDay timeOfDay,
        String description,
        List<StepItem> steps,
        LocalDateTime createdAt
) {
    public record StepItem(int order, Long cosmeticId, String cosmeticName) {}

    public static RoutineResult from(Routine routine) {
        List<StepItem> steps = routine.getRoutineCosmetics().stream()
                .map(rc -> new StepItem(rc.getOrder(), rc.getCosmetic().getId(), rc.getCosmetic().getName()))
                .toList();
        return new RoutineResult(
                routine.getId(),
                routine.getName(),
                routine.getTimeOfDay(),
                routine.getDescription(),
                steps,
                routine.getCreatedAt()
        );
    }
}
