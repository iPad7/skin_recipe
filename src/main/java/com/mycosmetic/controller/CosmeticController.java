package com.mycosmetic.controller;

import com.mycosmetic.dto.request.CosmeticRequest;
import com.mycosmetic.dto.request.OcrConfirmRequest;
import com.mycosmetic.dto.response.CosmeticResponse;
import com.mycosmetic.service.CosmeticService;
import com.mycosmetic.service.OcrService;
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
        return ResponseEntity.ok(cosmeticService.findAll(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<CosmeticResponse> save(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CosmeticRequest request) {
        return ResponseEntity.ok(cosmeticService.save(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CosmeticResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CosmeticRequest request) {
        return ResponseEntity.ok(cosmeticService.update(userDetails.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        cosmeticService.delete(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // confidence: high → CosmeticResponse, low → OcrParseResult
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
        CosmeticResponse response = cosmeticService.confirmOcr(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }
}
