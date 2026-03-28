package com.mycosmetic.service;

import com.mycosmetic.dto.request.CosmeticRequest;
import com.mycosmetic.dto.response.CosmeticResponse;
import com.mycosmetic.dto.response.OcrParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final UpstageOcrClient ocrClient;
    private final UpstageLlmClient llmClient;
    private final FileStorageService fileStorageService;
    private final CosmeticService cosmeticService;

    /**
     * 앞면 + 뒷면 사진을 받아 OCR → LLM 파싱까지 수행.
     * - confidence: high → 자동 저장 후 CosmeticResponse 반환
     * - confidence: low  → OcrParseResult 반환 (사용자 확인 대기)
     */
    public Object process(String email, MultipartFile frontImage, MultipartFile backImage) throws InterruptedException {
        // 1. 이미지 저장 (OCR 로그용)
        String frontUrl = fileStorageService.store(frontImage);
        String backUrl = fileStorageService.store(backImage);

        // 2. OCR 텍스트 추출 (앞면 + 뒷면 합산) — Tier 0 RPS 1 제한으로 1초 간격 필요
        String frontText = ocrClient.extractText(frontImage);
        Thread.sleep(1100);
        String backText = ocrClient.extractText(backImage);
        String combinedText = frontText + "\n" + backText;

        // 3. Solar LLM 파싱
        OcrParseResult result = llmClient.parseCosmetic(combinedText);

        // 4. confidence 분기
        if (result.isHighConfidence()) {
            CosmeticRequest request = toCosmeticRequest(result);
            return cosmeticService.save(email, request, frontUrl);
        } else {
            result.setImageUrl(frontUrl);
            return result;
        }
    }

    private CosmeticRequest toCosmeticRequest(OcrParseResult result) {
        try {
            CosmeticRequest request = new CosmeticRequest();
            setField(request, "name", result.getName());
            setField(request, "brand", result.getBrand());
            setField(request, "category", result.toCosmeticCategory());
            setField(request, "ingredients", result.getIngredients());
            return request;
        } catch (Exception e) {
            throw new RuntimeException("CosmeticRequest 변환 실패", e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
