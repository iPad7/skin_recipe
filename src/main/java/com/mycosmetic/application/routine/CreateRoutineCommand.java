package com.mycosmetic.application.routine;

import com.mycosmetic.domain.routine.TimeOfDay;

public record CreateRoutineCommand(
        TimeOfDay timeOfDay
) {}
