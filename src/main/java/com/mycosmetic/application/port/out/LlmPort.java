package com.mycosmetic.application.port.out;

import com.mycosmetic.application.cosmetic.OcrParseResult;
import com.mycosmetic.application.routine.RoutineLlmResult;
import com.mycosmetic.domain.chat.ChatMessage;
import com.mycosmetic.domain.cosmetic.Cosmetic;
import com.mycosmetic.domain.routine.TimeOfDay;
import com.mycosmetic.domain.user.User;

import java.util.List;

/**
 * LLM(Solar) 호출 outbound 포트.
 * 구현체: {@code adapter.out.upstage.UpstageLlmClient}
 */
public interface LlmPort {

    OcrParseResult parseCosmetic(String ocrText);

    RoutineLlmResult recommendRoutine(User user, List<Cosmetic> cosmetics, TimeOfDay timeOfDay);

    String chat(String systemPrompt, List<ChatMessage> history, String userMessage);
}
