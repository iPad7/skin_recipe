package com.mycosmetic.adapter.in.web.dto.request;

import com.mycosmetic.domain.routine.TimeOfDay;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoutineRequest {

    @NotNull
    private TimeOfDay timeOfDay;
}
