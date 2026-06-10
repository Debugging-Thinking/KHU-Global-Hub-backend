package com.khu.globalhub.identity.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 인증 전용 엔티티. 로그인/회원가입에 필요한 정보만 관리한다.
 * 프로필 정보(이름, 학과 등)는 Profile 엔티티에서 별도 관리한다.
 */
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    /** 관리자 계정 여부. 단일 운영자(ADMIN_EMAIL)에만 true. */
    @Column(name = "is_admin", nullable = false)
    @Builder.Default
    private Boolean isAdmin = false;

    // 이메일 인증 코드 (인증 완료 후 null로 초기화)
    private String emailVerificationCode;
    private LocalDateTime codeExpiredAt;

    // JWT Refresh Token (로그아웃 시 null로 초기화)
    private String refreshToken;
    private LocalDateTime refreshTokenExpiredAt;

    public void saveEmailVerificationCode(String code, LocalDateTime expiredAt) {
        this.emailVerificationCode = code;
        this.codeExpiredAt = expiredAt;
    }

    public void completeEmailVerification() {
        this.isEmailVerified = true;
        this.emailVerificationCode = null;
        this.codeExpiredAt = null;
    }

    public void updateRefreshToken(String token, LocalDateTime expiredAt) {
        this.refreshToken = token;
        this.refreshTokenExpiredAt = expiredAt;
    }

    public void logout() {
        this.refreshToken = null;
        this.refreshTokenExpiredAt = null;
    }

    /** 관리자 권한 부여(부팅 시 ADMIN_EMAIL 동기화용). 멱등. */
    public void grantAdmin() {
        this.isAdmin = true;
    }

    public void revokeAdmin() {
        this.isAdmin = false;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.emailVerificationCode = null;
        this.codeExpiredAt = null;
        this.refreshToken = null;
        this.refreshTokenExpiredAt = null;
    }
}
