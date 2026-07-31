package com.moveout.kb_backend.user.repository;

import com.moveout.kb_backend.user.entity.UserAgreement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgreementRepository extends JpaRepository<UserAgreement, UUID> {}
