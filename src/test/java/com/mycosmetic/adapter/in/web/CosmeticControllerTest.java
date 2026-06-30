package com.mycosmetic.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycosmetic.common.config.SecurityConfig;
import com.mycosmetic.application.cosmetic.CosmeticResult;
import com.mycosmetic.domain.cosmetic.Cosmetic;
import com.mycosmetic.domain.cosmetic.CosmeticCategory;
import com.mycosmetic.domain.user.SkinType;
import com.mycosmetic.domain.user.User;
import com.mycosmetic.common.security.JwtUtil;
import com.mycosmetic.common.security.UserDetailsServiceImpl;
import com.mycosmetic.application.cosmetic.CosmeticService;
import com.mycosmetic.application.cosmetic.OcrService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CosmeticController.class)
@Import(SecurityConfig.class)
class CosmeticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CosmeticService cosmeticService;

    @MockitoBean
    private OcrService ocrService;

    // Filter 자체가 아닌 의존성만 Mock — 실제 필터가 동작해야 Security 체인이 유지됨
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    // ── 인증 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("JWT 없이 접근하면 401을 반환한다")
    void unauthorized_withoutToken() throws Exception {
        mockMvc.perform(get("/cosmetics"))
                .andExpect(status().isUnauthorized());
    }

    // ── 조회 ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("내 화장품 목록을 조회한다")
    void findAll() throws Exception {
        given(cosmeticService.findAll(anyString())).willReturn(List.of(makeCosmeticResult()));

        mockMvc.perform(get("/cosmetics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("토너"));
    }

    // ── 등록 ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("화장품을 수동 등록한다")
    void save() throws Exception {
        given(cosmeticService.save(anyString(), any())).willReturn(makeCosmeticResult());

        Map<String, Object> body = Map.of(
                "name", "토너",
                "category", "SKIN"
        );

        mockMvc.perform(post("/cosmetics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("토너"));
    }

    @Test
    @WithMockUser
    @DisplayName("name이 없으면 400을 반환한다")
    void save_missingName() throws Exception {
        Map<String, Object> body = Map.of("category", "SKIN");

        mockMvc.perform(post("/cosmetics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── 수정 ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("화장품을 수정한다")
    void update() throws Exception {
        given(cosmeticService.update(anyString(), any(), any())).willReturn(makeCosmeticResult());

        Map<String, Object> body = Map.of("name", "토너", "category", "SKIN");

        mockMvc.perform(put("/cosmetics/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    // ── 삭제 ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("화장품을 삭제하면 204를 반환한다")
    void deleteCosmetic() throws Exception {
        mockMvc.perform(delete("/cosmetics/1"))
                .andExpect(status().isNoContent());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────

    private CosmeticResult makeCosmeticResult() {
        User user = User.builder()
                .email("user@example.com").password("encoded")
                .nickname("테스터").skinType(SkinType.NORMAL).build();
        Cosmetic cosmetic = Cosmetic.builder()
                .user(user).name("토너").brand("이니스프리")
                .category(CosmeticCategory.SKIN).ingredients("정제수").build();
        return CosmeticResult.from(cosmetic);
    }
}
