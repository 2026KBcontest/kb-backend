package com.moveout.kb_backend.user.service;

import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.user.dto.UserMeResponse;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateMonthlyIncome(UUID userId, Long monthlyIncome) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("USER_001", "존재하지 않는 사용자입니다."));
        user.updateMonthlyIncome(monthlyIncome);
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMe(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("USER_001", "존재하지 않는 사용자입니다."));
        return UserMeResponse.from(user);
    }
}
