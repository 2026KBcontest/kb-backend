package com.moveout.kb_backend.policy.repository;

import com.moveout.kb_backend.policy.entity.YouthPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YouthPolicyRepository extends JpaRepository<YouthPolicy, Long> {

    // 정책 ID(bizId) 기반 중복 체크용 메서드
    Optional<YouthPolicy> findByBizId(String bizId);
}