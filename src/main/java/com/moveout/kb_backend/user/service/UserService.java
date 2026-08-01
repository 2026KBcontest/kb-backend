package com.moveout.kb_backend.user.service;

import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.forecast.repository.SimulationResultRepository;
import com.moveout.kb_backend.mydata.repository.MyDataSnapshotRepository;
import com.moveout.kb_backend.user.dto.ChangePasswordRequest;
import com.moveout.kb_backend.user.dto.UpdateProfileRequest;
import com.moveout.kb_backend.user.dto.UserMeResponse;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.repository.UserAgreementRepository;
import com.moveout.kb_backend.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MyDataSnapshotRepository myDataSnapshotRepository;
    private final SimulationResultRepository simulationResultRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public UserMeResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("USER_001", "존재하지 않는 사용자입니다."));

        String newEmail = request.getEmail();
        if (newEmail != null && !newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new BusinessException("AUTH_002", "이미 존재하는 이메일입니다.", HttpStatus.CONFLICT);
        }

        user.updateProfile(
                request.getName(),
                newEmail,
                request.getBirthDate(),
                request.getResidenceRegion(),
                request.getGender(),
                request.getJob(),
                request.getPhone(),
                // 월 소득은 PATCH /api/users/me/income 에서, 자산·희망지역은 아직 화면이 없어
                // 프로필 수정으로는 건드리지 않는다. null 이면 기존 값이 유지된다.
                null,
                null,
                null,
                request.getMonthlySavingGoal());

        return UserMeResponse.from(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("USER_001", "존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("AUTH_003", "현재 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        user.updateRefreshToken(null);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("USER_001", "존재하지 않는 사용자입니다."));

        myDataSnapshotRepository.findByUser(user).ifPresent(myDataSnapshotRepository::delete);
        simulationResultRepository.findById(userId).ifPresent(simulationResultRepository::delete);
        userAgreementRepository.findById(userId).ifPresent(userAgreementRepository::delete);
        userRepository.delete(user);
    }
}
