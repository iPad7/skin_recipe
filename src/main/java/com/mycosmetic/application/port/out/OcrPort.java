package com.mycosmetic.application.port.out;

import org.springframework.web.multipart.MultipartFile;

/**
 * OCR(Document Parse) 호출 outbound 포트.
 * 구현체: {@code adapter.out.upstage.UpstageOcrClient}
 */
public interface OcrPort {

    String extractText(MultipartFile file);
}
