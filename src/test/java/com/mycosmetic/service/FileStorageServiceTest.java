package com.mycosmetic.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("파일을 저장하면 /uploads/ 경로를 반환한다")
    void store_returnsUploadPath() throws IOException {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-image-data".getBytes()
        );

        String path = service.store(file);

        assertThat(path).startsWith("/uploads/");
        assertThat(path).endsWith(".jpg");
    }

    @Test
    @DisplayName("파일명은 UUID 기반으로 생성되어 중복되지 않는다")
    void store_uniqueFilenames() throws IOException {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-image-data".getBytes()
        );

        String path1 = service.store(file);
        String path2 = service.store(file);

        assertThat(path1).isNotEqualTo(path2);
    }

    @Test
    @DisplayName("저장된 파일이 실제로 디스크에 존재한다")
    void store_fileExistsOnDisk() throws IOException {
        FileStorageService service = new FileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", "fake-image-data".getBytes()
        );

        String path = service.store(file);
        String filename = path.replace("/uploads/", "");

        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
    }
}
