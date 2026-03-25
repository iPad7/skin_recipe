package com.mycosmetic.controller;

import com.mycosmetic.dto.request.RoutineRequest;
import com.mycosmetic.dto.response.RoutineResponse;
import com.mycosmetic.service.RoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService routineService;

    @PostMapping
    public ResponseEntity<RoutineResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RoutineRequest request) {
        return ResponseEntity.ok(routineService.create(userDetails.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<RoutineResponse>> findAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(routineService.findAll(userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        routineService.delete(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
