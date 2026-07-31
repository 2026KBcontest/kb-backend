package com.moveout.kb_backend.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAgreement {

    @Id
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private boolean privacyAgreed;
    private LocalDateTime privacyAgreedAt;

    private boolean mydataAgreed;
    private LocalDateTime mydataAgreedAt;

    private boolean marketingAgreed;
    private LocalDateTime marketingAgreedAt;

    public UserAgreement(User user, boolean privacyAgreed, boolean mydataAgreed, boolean marketingAgreed) {
        this.user = user;
        LocalDateTime now = LocalDateTime.now();
        this.privacyAgreed = privacyAgreed;
        this.privacyAgreedAt = privacyAgreed ? now : null;
        this.mydataAgreed = mydataAgreed;
        this.mydataAgreedAt = mydataAgreed ? now : null;
        this.marketingAgreed = marketingAgreed;
        this.marketingAgreedAt = marketingAgreed ? now : null;
    }
}
