package com.mycosmetic.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpstageOcrClient {

    private final WebClient upstageWebClient;

    @Value("${upstage.model.document-parse}")
    private String documentParseModel;

    public String extractText(MultipartFile file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("model", documentParseModel);
        builder.part("ocr", "force");

        try {
            builder.part("document", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패", e);
        }

        Map<?, ?> response = upstageWebClient.post()
                .uri("/v1/document-digitization")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        log.info("OCR raw 응답 [{}]: {}", file.getOriginalFilename(), response);
        String text = parseText(response);
        log.info("OCR 추출 텍스트 [{}]: {}", file.getOriginalFilename(), text);
        return text;
    }

    @SuppressWarnings("unchecked")
    private String parseText(Map<?, ?> response) {
        if (response == null) return "";

        // 최상위 content.html에서 HTML 태그 제거하여 전체 텍스트 추출
        Map<String, Object> content = (Map<String, Object>) response.get("content");
        if (content != null) {
            String html = (String) content.get("html");
            if (html != null && !html.isBlank()) {
                return html.replaceAll("<[^>]+>", " ")
                           .replaceAll("\\s+", " ")
                           .trim();
            }
        }
        return "";
    }
}
