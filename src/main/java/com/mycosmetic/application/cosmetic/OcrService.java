package com.mycosmetic.application.cosmetic;

import com.mycosmetic.application.port.out.FileStoragePort;
import com.mycosmetic.application.port.out.LlmPort;
import com.mycosmetic.application.port.out.OcrPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OcrService {

    private final OcrPort ocrClient;
    private final LlmPort llmClient;
    private final FileStoragePort fileStorageService;
    private final CosmeticService cosmeticService;

    /**
     * 앞면 + 뒷면 사진을 받아 OCR → LLM 파싱까지 수행.
     * - confidence: high → 자동 저장 후 CosmeticResult 반환
     * - confidence: low  → OcrParseResult 반환 (사용자 확인 대기)
     */
    public Object process(String email, MultipartFile frontImage, MultipartFile backImage) throws InterruptedException {
        // 1. 이미지 저장 (OCR 로그용)
        String frontUrl = fileStorageService.store(frontImage);
        fileStorageService.store(backImage);

        // 2. OCR 텍스트 추출 (앞면 + 뒷면 합산) — Tier 0 RPS 1 제한으로 1초 간격 필요
        String frontText = ocrClient.extractText(frontImage);
        Thread.sleep(1100);
        String backText = ocrClient.extractText(backImage);
        String combinedText = frontText + "\n" + backText;

        // 3. Solar LLM 파싱
        OcrParseResult result = llmClient.parseCosmetic(combinedText);

        // 4. confidence 분기
        if (result.isHighConfidence()) {
            SaveCosmeticCommand command = new SaveCosmeticCommand(
                    result.getName(), result.getBrand(), result.toCosmeticCategory(),
                    result.getIngredients(), frontUrl);
            return cosmeticService.save(email, command);
        } else {
            result.setImageUrl(frontUrl);
            return result;
        }
    }
}
