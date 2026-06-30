package com.mycosmetic.adapter.in.web.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RoutineLlmResult {
    private String name;
    private String description;
    private List<StepItem> steps;

    @Getter
    @NoArgsConstructor
    public static class StepItem {
        private Long cosmeticId;
        private int order;
    }
}
