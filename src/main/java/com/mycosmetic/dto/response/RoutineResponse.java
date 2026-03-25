package com.mycosmetic.dto.response;

import com.mycosmetic.entity.Routine;
import com.mycosmetic.entity.TimeOfDay;
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

    public RoutineResponse(Routine routine) {
        this.id = routine.getId();
        this.name = routine.getName();
        this.timeOfDay = routine.getTimeOfDay();
        this.description = routine.getDescription();
        this.steps = routine.getRoutineCosmetics().stream()
                .map(rc -> new RoutineCosmeticItem(rc.getOrder(), rc.getCosmetic().getId(), rc.getCosmetic().getName()))
                .toList();
        this.createdAt = routine.getCreatedAt();
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
