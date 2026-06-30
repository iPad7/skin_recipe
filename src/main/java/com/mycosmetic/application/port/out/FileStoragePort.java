package com.mycosmetic.application.port.out;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장 outbound 포트.
 * 구현체: {@code adapter.out.storage.FileStorageService}
 */
public interface FileStoragePort {

    String store(MultipartFile file);
}
