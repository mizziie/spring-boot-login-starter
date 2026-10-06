package com.example.javalogin.controller;

import com.example.javalogin.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, String>>> dashboard() {
        return ResponseEntity.ok(
                ApiResponse.success("Admin dashboard", Map.of("status", "ok", "message", "Admin access granted"))
        );
    }
}
