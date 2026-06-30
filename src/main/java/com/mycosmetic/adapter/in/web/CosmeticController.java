package com.mycosmetic.adapter.in.web;

import com.mycosmetic.adapter.in.web.dto.request.CosmeticRequest;
import com.mycosmetic.adapter.in.web.dto.request.OcrConfirmRequest;
import com.mycosmetic.adapter.in.web.dto.response.CosmeticResponse;
import com.mycosmetic.application.cosmetic.CosmeticResult;
import com.mycosmetic.application.cosmetic.CosmeticService;
import com.mycosmetic.application.cosmetic.OcrService;
import com.mycosmetic.application.cosmetic.SaveCosmeticCommand;
import com.mycosmetic.application.cosmetic.UpdateCosmeticCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/cosmetics")
@RequiredArgsConstructor
public class CosmeticController {

    private final CosmeticService cosmeticService;
    private final OcrService ocrService;

    @GetMapping
    public ResponseEntity<List<CosmeticResponse>> findAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CosmeticResponse> body = cosmeticService.findAll(userDetails.getUsername()).stream()
                .map(CosmeticResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<CosmeticResponse> save(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CosmeticRequest request) {
        CosmeticResult result = cosmeticService.save(userDetails.getUsername(), new SaveCosmeticCommand(
                request.getName(), request.getBrand(), request.getCategory(), request.getIngredients(), null));
        return ResponseEntity.ok(CosmeticResponse.from(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CosmeticResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CosmeticRequest request) {
        CosmeticResult result = cosmeticService.update(userDetails.getUsername(), id, new UpdateCosmeticCommand(
                request.getName(), request.getBrand(), request.getCategory(), request.getIngredients()));
        return ResponseEntity.ok(CosmeticResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        cosmeticService.delete(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // confidence: high → CosmeticResult, low → OcrParseResult (둘 다 application 모델 직접 직렬화)
    @PostMapping("/ocr")
    public ResponseEntity<Object> ocr(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage) throws InterruptedException {
        Object result = ocrService.process(userDetails.getUsername(), frontImage, backImage);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ocr/confirm")
    public ResponseEntity<CosmeticResponse> confirmOcr(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OcrConfirmRequest request) {
        CosmeticResult result = cosmeticService.confirmOcr(userDetails.getUsername(), new SaveCosmeticCommand(
                request.getName(), request.getBrand(), request.getCategory(),
                request.getIngredients(), request.getImageUrl()));
        return ResponseEntity.ok(CosmeticResponse.from(result));
    }
}
