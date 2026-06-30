package com.mycosmetic.application.cosmetic;
import com.mycosmetic.application.port.out.OcrPort;
import com.mycosmetic.application.port.out.LlmPort;
import com.mycosmetic.application.port.out.FileStoragePort;

import com.mycosmetic.adapter.in.web.dto.request.CosmeticRequest;
import com.mycosmetic.adapter.in.web.dto.response.CosmeticResponse;
import com.mycosmetic.application.cosmetic.OcrParseResult;
import com.mycosmetic.domain.cosmetic.Cosmetic;
import com.mycosmetic.domain.cosmetic.CosmeticCategory;
import com.mycosmetic.domain.user.SkinType;
import com.mycosmetic.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    @InjectMocks
    private OcrService ocrService;

    @Mock
    private OcrPort ocrClient;

    @Mock
    private LlmPort llmClient;

    @Mock
    private FileStoragePort fileStorageService;

    @Mock
    private CosmeticService cosmeticService;

    @Test
    @DisplayName("confidence가 high이면 자동 저장하고 CosmeticResponse를 반환한다")
    void process_highConfidence_autoSave() throws InterruptedException {
        given(fileStorageService.store(any())).willReturn("/uploads/front.jpg");
        given(ocrClient.extractText(any())).willReturn("OCR 텍스트");
        given(llmClient.parseCosmetic(anyString())).willReturn(makeParseResult("high"));
        given(cosmeticService.save(anyString(), any(CosmeticRequest.class), anyString()))
                .willReturn(makeCosmeticResponse());

        Object result = ocrService.process("user@example.com", mockFile("front"), mockFile("back"));

        assertThat(result).isInstanceOf(CosmeticResponse.class);
        verify(cosmeticService).save(anyString(), any(CosmeticRequest.class), anyString());
    }

    @Test
    @DisplayName("confidence가 low이면 저장하지 않고 OcrParseResult를 반환한다")
    void process_lowConfidence_returnForReview() throws InterruptedException {
        given(fileStorageService.store(any())).willReturn("/uploads/front.jpg");
        given(ocrClient.extractText(any())).willReturn("OCR 텍스트");
        given(llmClient.parseCosmetic(anyString())).willReturn(makeParseResult("low"));

        Object result = ocrService.process("user@example.com", mockFile("front"), mockFile("back"));

        assertThat(result).isInstanceOf(OcrParseResult.class);
        verify(cosmeticService, never()).save(anyString(), any(CosmeticRequest.class), anyString());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────

    private OcrParseResult makeParseResult(String confidence) {
        try {
            OcrParseResult result = new OcrParseResult();
            setField(result, "name", "토너");
            setField(result, "brand", "이니스프리");
            setField(result, "category", "SKIN");
            setField(result, "ingredients", "정제수, 부틸렌글라이콜");
            setField(result, "confidence", confidence);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CosmeticResponse makeCosmeticResponse() {
        User user = User.builder()
                .email("user@example.com").password("encoded")
                .nickname("테스터").skinType(SkinType.NORMAL).build();
        Cosmetic cosmetic = Cosmetic.builder()
                .user(user).name("토너").brand("이니스프리")
                .category(CosmeticCategory.SKIN).ingredients("정제수").build();
        return new CosmeticResponse(cosmetic);
    }

    private MockMultipartFile mockFile(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", "fake-image".getBytes());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
