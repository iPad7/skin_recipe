package com.mycosmetic.service;

import com.mycosmetic.dto.request.CosmeticRequest;
import com.mycosmetic.dto.request.OcrConfirmRequest;
import com.mycosmetic.dto.response.CosmeticResponse;
import com.mycosmetic.entity.Cosmetic;
import com.mycosmetic.entity.User;
import com.mycosmetic.repository.CosmeticRepository;
import com.mycosmetic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CosmeticService {

    private final CosmeticRepository cosmeticRepository;
    private final UserRepository userRepository;

    public List<CosmeticResponse> findAll(String email) {
        User user = getUser(email);
        return cosmeticRepository.findAllByUserId(user.getId()).stream()
                .map(CosmeticResponse::new)
                .toList();
    }

    public CosmeticResponse save(String email, CosmeticRequest request) {
        return save(email, request, null);
    }

    public CosmeticResponse save(String email, CosmeticRequest request, String imageUrl) {
        User user = getUser(email);
        Cosmetic cosmetic = Cosmetic.builder()
                .user(user)
                .name(request.getName())
                .brand(request.getBrand())
                .category(request.getCategory())
                .ingredients(request.getIngredients())
                .imageUrl(imageUrl)
                .build();
        return new CosmeticResponse(cosmeticRepository.save(cosmetic));
    }

    @Transactional
    public CosmeticResponse update(String email, Long cosmeticId, CosmeticRequest request) {
        Cosmetic cosmetic = getOwnedCosmetic(email, cosmeticId);
        cosmetic.update(request.getName(), request.getBrand(), request.getCategory(), request.getIngredients());
        return new CosmeticResponse(cosmetic);
    }

    public void delete(String email, Long cosmeticId) {
        Cosmetic cosmetic = getOwnedCosmetic(email, cosmeticId);
        cosmeticRepository.delete(cosmetic);
    }

    public CosmeticResponse confirmOcr(String email, OcrConfirmRequest request) {
        User user = getUser(email);
        Cosmetic cosmetic = Cosmetic.builder()
                .user(user)
                .name(request.getName())
                .brand(request.getBrand())
                .category(request.getCategory())
                .ingredients(request.getIngredients())
                .imageUrl(request.getImageUrl())
                .build();
        return new CosmeticResponse(cosmeticRepository.save(cosmetic));
    }

    // 소유자 검증 — 다른 사용자의 화장품 접근 차단
    private Cosmetic getOwnedCosmetic(String email, Long cosmeticId) {
        User user = getUser(email);
        Cosmetic cosmetic = cosmeticRepository.findById(cosmeticId)
                .orElseThrow(() -> new IllegalArgumentException("화장품을 찾을 수 없습니다."));
        if (!cosmetic.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return cosmetic;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
