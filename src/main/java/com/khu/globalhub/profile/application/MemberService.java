package com.khu.globalhub.profile.application;

import com.khu.globalhub.profile.presentation.dto.ProfileResponse;
import com.khu.globalhub.profile.presentation.dto.UpdateMentoringRoleRequest;
import com.khu.globalhub.profile.presentation.dto.UpdateProfileRequest;
import com.khu.globalhub.profile.domain.Profile;
import com.khu.globalhub.shared.port.MemberQueryPort;
import com.khu.globalhub.profile.infrastructure.ProfileRepository;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.enums.MentoringRole;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.infra.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final ProfileRepository profileRepository;
    private final MemberQueryPort memberQueryPort;
    private final S3Service s3Service;

    /** 프로필 응답 조립용 이메일 조회 (identity BC를 ID로만 참조). */
    private String resolveEmail(Long memberId) {
        return memberQueryPort.findEmail(memberId).orElse(null);
    }

    /**
     * 최초 프로필 생성 (POST /api/auth/profile). identity의 ProfileGateway가 위임.
     * - 신입생(입학년도==현재년도): MENTEE만 가능
     * - 재학생: MENTOR/MENTEE 선택 (NONE은 프로필 수정에서만)
     */
    @Transactional
    public void createProfile(Long memberId, String name, String department, String nationality,
                              Integer admissionYear, Language language, MentoringRole mentoringRole) {
        if (profileRepository.existsByMemberId(memberId)) {
            throw new CustomException(ErrorCode.PROFILE_ALREADY_EXISTS);
        }
        if (mentoringRole == MentoringRole.NONE) {
            throw new CustomException(ErrorCode.CANNOT_SET_NONE_ON_INIT);
        }
        boolean isNewStudent = admissionYear == LocalDate.now().getYear();
        if (isNewStudent && mentoringRole == MentoringRole.MENTOR) {
            throw new CustomException(ErrorCode.INVALID_MENTORING_ROLE);
        }

        Profile profile = Profile.builder()
                .memberId(memberId)
                .name(name)
                .department(department)
                .nationality(nationality)
                .admissionYear(admissionYear)
                .language(language)
                .mentoringRole(mentoringRole)
                .build();
        profileRepository.save(profile);
    }

    /** 프로필 조회 (본인 또는 타인). */
    public ProfileResponse getProfile(Long targetMemberId) {
        Profile profile = profileRepository.findByMemberId(targetMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));
        return ProfileResponse.from(profile, resolveEmail(profile.getMemberId()));
    }

    /** 프로필 수정 (본인만). */
    @Transactional
    public ProfileResponse updateProfile(Long memberId, UpdateProfileRequest req) {
        Profile profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        // 신입생(입학년도 == 현재년도)은 MENTEE 고정 — PATCH /me/mentoring-role 과 동일 규칙 (우회 방지)
        int currentYear = LocalDate.now().getYear();
        boolean isNewStudent = req.admissionYear() == currentYear;
        if (isNewStudent && req.mentoringRole() != MentoringRole.MENTEE) {
            throw new CustomException(ErrorCode.INVALID_MENTORING_ROLE);
        }

        profile.updateProfile(
                req.name(),
                req.department(),
                req.nationality(),
                req.admissionYear(),
                req.language()
        );
        profile.updateMentoringRole(req.mentoringRole());
        return ProfileResponse.from(profile, resolveEmail(profile.getMemberId()));
    }

    /** 프로필 이미지 업로드 및 URL 저장. */
    @Transactional
    public String updateProfileImage(Long memberId, byte[] imageBytes, String contentType) {
        Profile profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        String url = s3Service.uploadProfileImage(memberId, imageBytes, contentType);
        profile.updateProfileImage(url);
        return url;
    }

    /**
     * 멘토링 역할 변경 (본인만).
     * 신입생(입학년도 == 현재년도)은 MENTEE 고정 — 변경 불가.
     */
    @Transactional
    public ProfileResponse updateMentoringRole(Long memberId, UpdateMentoringRoleRequest req) {
        Profile profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        int currentYear = LocalDate.now().getYear();
        boolean isNewStudent = profile.getAdmissionYear() == currentYear;

        if (isNewStudent && req.mentoringRole() != MentoringRole.MENTEE) {
            throw new CustomException(ErrorCode.INVALID_MENTORING_ROLE);
        }

        profile.updateMentoringRole(req.mentoringRole());
        return ProfileResponse.from(profile, resolveEmail(profile.getMemberId()));
    }
}
