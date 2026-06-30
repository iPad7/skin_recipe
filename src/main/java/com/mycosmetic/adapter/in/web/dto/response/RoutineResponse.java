package com.mycosmetic.adapter.in.web.dto.response;

import com.mycosmetic.application.routine.RoutineResult;
import com.mycosmetic.domain.routine.TimeOfDay;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class RoutineResponse {
    private final Long id;
    private final String name;
    private final TimeOfDay timeOfDay;
    private final String description;
    private final List<RoutineCosmeticItem> steps;
    private final LocalDateTime createdAt;

    private RoutineResponse(Long id, String name, TimeOfDay timeOfDay, String description,
                            List<RoutineCosmeticItem> steps, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.timeOfDay = timeOfDay;
        this.description = description;
        this.steps = steps;
        this.createdAt = createdAt;
    }

    public static RoutineResponse from(RoutineResult result) {
        List<RoutineCosmeticItem> steps = result.steps().stream()
                .map(s -> new RoutineCosmeticItem(s.order(), s.cosmeticId(), s.cosmeticName()))
                .toList();
        return new RoutineResponse(result.id(), result.name(), result.timeOfDay(),
                result.description(), steps, result.createdAt());
    }

    @Getter
    public static class RoutineCosmeticItem {
        private final int order;
        private final Long cosmeticId;
        private final String cosmeticName;

        public RoutineCosmeticItem(int order, Long cosmeticId, String cosmeticName) {
            this.order = order;
            this.cosmeticId = cosmeticId;
            this.cosmeticName = cosmeticName;
        }
    }
}
