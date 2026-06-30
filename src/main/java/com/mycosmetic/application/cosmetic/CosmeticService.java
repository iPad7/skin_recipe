package com.mycosmetic.application.cosmetic;

import com.mycosmetic.application.port.out.CosmeticRepository;
import com.mycosmetic.application.port.out.RoutineCosmeticRepository;
import com.mycosmetic.application.port.out.UserRepository;
import com.mycosmetic.application.port.out.VectorStorePort;
import com.mycosmetic.domain.cosmetic.Cosmetic;
import com.mycosmetic.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CosmeticService {

    private final CosmeticRepository cosmeticRepository;
    private final UserRepository userRepository;
    private final VectorStorePort vectorStoreService;
    private final RoutineCosmeticRepository routineCosmeticRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void loadVectorsOnStartup() {
        List<Cosmetic> all = cosmeticRepository.findAll();
        int success = 0;
        for (Cosmetic c : all) {
            try {
                vectorStoreService.addVector(c.getId(), toEmbedText(c));
                success++;
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                log.warn("벡터 로드 실패 — cosmeticId={}, status={}, body={}",
                        c.getId(), e.getStatusCode(), e.getResponseBodyAsString());
            } catch (Exception e) {
                log.warn("벡터 로드 실패 — cosmeticId={}, reason={}", c.getId(), e.getMessage());
            }
        }
        log.info("벡터 스토어 초기화 완료: {}/{} 건 로드됨", success, all.size());
    }

    public List<CosmeticResult> findAll(String email) {
        User user = getUser(email);
        return cosmeticRepository.findAllByUserId(user.getId()).stream()
                .map(CosmeticResult::from)
                .toList();
    }

    public CosmeticResult save(String email, SaveCosmeticCommand command) {
        User user = getUser(email);
        Cosmetic cosmetic = Cosmetic.builder()
                .user(user)
                .name(command.name())
                .brand(command.brand())
                .category(command.category())
                .ingredients(command.ingredients())
                .imageUrl(command.imageUrl())
                .build();
        Cosmetic saved = cosmeticRepository.save(cosmetic);
        vectorStoreService.addVector(saved.getId(), toEmbedText(saved));
        return CosmeticResult.from(saved);
    }

    @Transactional
    public CosmeticResult update(String email, Long cosmeticId, UpdateCosmeticCommand command) {
        Cosmetic cosmetic = getOwnedCosmetic(email, cosmeticId);
        cosmetic.update(command.name(), command.brand(), command.category(), command.ingredients());
        vectorStoreService.addVector(cosmetic.getId(), toEmbedText(cosmetic));
        return CosmeticResult.from(cosmetic);
    }

    public void delete(String email, Long cosmeticId) {
        Cosmetic cosmetic = getOwnedCosmetic(email, cosmeticId);
        if (routineCosmeticRepository.existsByCosmeticId(cosmeticId)) {
            throw new IllegalArgumentException("루틴에 포함된 화장품은 삭제할 수 없습니다. 먼저 루틴에서 제거해주세요.");
        }
        cosmeticRepository.delete(cosmetic);
        vectorStoreService.removeVector(cosmeticId);
    }

    /** OCR 확인(HITL) 저장 — save와 동일 동작 */
    public CosmeticResult confirmOcr(String email, SaveCosmeticCommand command) {
        return save(email, command);
    }

    private String toEmbedText(Cosmetic c) {
        return c.getName() + " " + c.getBrand() + " " + c.getIngredients();
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
