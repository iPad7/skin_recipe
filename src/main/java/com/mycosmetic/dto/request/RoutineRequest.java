package com.mycosmetic.dto.request;

import com.mycosmetic.entity.TimeOfDay;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoutineRequest {

    @NotNull
    private TimeOfDay timeOfDay;
}
