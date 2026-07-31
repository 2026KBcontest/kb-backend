package com.moveout.kb_backend.user.controller;

import com.moveout.kb_backend.user.dto.UpdateIncomeRequest;
import com.moveout.kb_backend.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/me/income")
    public void updateIncome(Authentication authentication, @Valid @RequestBody UpdateIncomeRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.updateMonthlyIncome(userId, request.getMonthlyIncome());
    }
}
