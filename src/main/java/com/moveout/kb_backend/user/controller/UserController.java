package com.moveout.kb_backend.user.controller;

import com.moveout.kb_backend.user.dto.ChangePasswordRequest;
import com.moveout.kb_backend.user.dto.UpdateIncomeRequest;
import com.moveout.kb_backend.user.dto.UpdateProfileRequest;
import com.moveout.kb_backend.user.dto.UserMeResponse;
import com.moveout.kb_backend.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserMeResponse getMe(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return userService.getMe(userId);
    }

    @PatchMapping("/me")
    public UserMeResponse updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return userService.updateProfile(userId, request);
    }

    @PatchMapping("/me/income")
    public void updateIncome(Authentication authentication, @Valid @RequestBody UpdateIncomeRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.updateMonthlyIncome(userId, request.getMonthlyIncome());
    }

    @PatchMapping("/me/password")
    public void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.changePassword(userId, request);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }
}
