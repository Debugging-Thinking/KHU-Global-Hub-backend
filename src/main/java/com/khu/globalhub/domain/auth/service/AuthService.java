package com.khu.globalhub.domain.auth.service;

import com.khu.globalhub.domain.auth.dto.*;
import com.khu.globalhub.domain.member.entity.Member;
import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.domain.member.repository.MemberRepository;
import com.khu.globalhub.domain.member.repository.ProfileRepository;
import com.khu.globalhub.shared.enums.MentoringRole;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    /** 회원가입: @khu.ac.kr 검증 → Member 저장 → 인증 코드 이메일 발송 */
    public void register(RegisterRequest request) {
        if (!request.getEmail().endsWith("@khu.ac.kr")) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_DOMAIN);
        }
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String code = generateVerificationCode();
        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        member.saveEmailVerificationCode(code, LocalDateTime.now().plusMinutes(5));
        memberRepository.save(member);

        emailService.sendVerificationEmail(request.getEmail(), code);
    }

    /** 이메일 인증: 코드 검증 → is_email_verified=true → JWT 발급 */
    public LoginResponse verifyEmail(VerifyEmailRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getCodeExpiredAt() == null || member.getCodeExpiredAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!member.getEmailVerificationCode().equals(request.getCode())) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        member.completeEmailVerification();

        return issueTokens(member);
    }

    /**
     * 프로필 생성: 이메일 인증 후 필수 단계.
     * - 신입생(입학년도==현재년도): MENTEE만 가능
     * - 재학생: MENTOR/MENTEE 선택 (NONE은 프로필 수정에서만)
     */
    public void createProfile(Long memberId, CreateProfileRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (profileRepository.existsByMemberId(member.getId())) {
            throw new CustomException(ErrorCode.PROFILE_ALREADY_EXISTS);
        }
        if (request.getMentoringRole() == MentoringRole.NONE) {
            throw new CustomException(ErrorCode.CANNOT_SET_NONE_ON_INIT);
        }

        boolean isNewStudent = request.getAdmissionYear() == LocalDate.now().getYear();
        if (isNewStudent && request.getMentoringRole() == MentoringRole.MENTOR) {
            throw new CustomException(ErrorCode.INVALID_MENTORING_ROLE);
        }

        Profile profile = Profile.builder()
                .memberId(member.getId())
                .name(request.getName())
                .department(request.getDepartment())
                .nationality(request.getNationality())
                .admissionYear(request.getAdmissionYear())
                .language(request.getLanguage())
                .mentoringRole(request.getMentoringRole())
                .build();
        profileRepository.save(profile);
    }

    /** 로그인: 이메일 인증 + 비밀번호 검증 → JWT 발급 */
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (!member.getIsEmailVerified()) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        return issueTokens(member);
    }

    /** Refresh Token으로 Access Token 재발급 */
    public LoginResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (!refreshToken.equals(member.getRefreshToken()) ||
                member.getRefreshTokenExpiredAt() == null ||
                member.getRefreshTokenExpiredAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        return issueTokens(member);
    }

    /** 로그아웃: DB의 Refresh Token 제거 */
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        member.logout();
    }

    /**
     * 비밀번호 재설정 코드 발송.
     * 이메일 인증을 마친 계정에만 발송 (회원가입 미완료 계정은 어차피 로그인 불가).
     * 인증 코드 필드(emailVerificationCode)를 재활용한다.
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (!member.getIsEmailVerified()) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        String code = generateVerificationCode();
        member.saveEmailVerificationCode(code, LocalDateTime.now().plusMinutes(10));

        emailService.sendPasswordResetEmail(request.getEmail(), code);
    }

    /**
     * 비밀번호 재설정 실행.
     * 성공 시 기존 Refresh Token도 무효화하여 모든 기기 강제 로그아웃.
     */
    public void resetPassword(ResetPasswordRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getCodeExpiredAt() == null || member.getCodeExpiredAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!request.getCode().equals(member.getEmailVerificationCode())) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        member.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    private LoginResponse issueTokens(Member member) {
        String accessToken = jwtTokenProvider.generateAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());
        member.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(7));

        boolean hasProfile = profileRepository.existsByMemberId(member.getId());
        return LoginResponse.of(accessToken, refreshToken, hasProfile);
    }

    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }
}
