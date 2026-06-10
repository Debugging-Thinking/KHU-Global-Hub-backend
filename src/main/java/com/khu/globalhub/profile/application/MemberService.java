package com.khu.globalhub.profile.application;

import com.khu.globalhub.profile.presentation.dto.ProfileResponse;
import com.khu.globalhub.profile.presentation.dto.MemberSearchResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                              Integer admissionYear, Language language, String preferredLanguage,
                              MentoringRole mentoringRole, String bio) {
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

        String pref = resolvePreferred(preferredLanguage, language);
        Language bucket = toBucket(pref);

        Profile profile = Profile.builder()
                .memberId(memberId)
                .name(name)
                .department(department)
                .nationality(nationality)
                .admissionYear(admissionYear)
                .language(bucket)
                .preferredLanguage(pref)
                .mentoringRole(mentoringRole)
                .bio(bio)
                .build();
        profileRepository.save(profile);
    }

    /**
     * 선호 언어 코드 결정: 제공되면 그 값, 아니면(레거시/하위호환) language enum의 Azure 코드.
     */
    private String resolvePreferred(String preferredLanguage, Language language) {
        return (preferredLanguage != null && !preferredLanguage.isBlank())
                ? preferredLanguage
                : language.toAzureCode();
    }

    /**
     * Azure 코드 → 6개 언어 버킷. 지원 6개 코드(ko/en/zh-Hans/vi/uz/mn-Cyrl)와 정확히 일치하면 그 언어,
     * 그 외(예: fr, ja, zh-Hant, mni)는 EN — 정적 UI=영어 + 콘텐츠=원문+on-demand 번역 대상.
     * (source 감지용 {@code fromAzureCode}의 접두사 매칭과 달리 버킷은 엄격 매칭한다.)
     */
    private Language toBucket(String preferredCode) {
        if (preferredCode == null) return Language.EN;
        for (Language l : Language.values()) {
            if (l.toAzureCode().equalsIgnoreCase(preferredCode)) return l;
        }
        return Language.EN;
    }

    /** 프로필 조회 (본인 또는 타인). */
    public ProfileResponse getProfile(Long targetMemberId) {
        Profile profile = profileRepository.findByMemberId(targetMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));
        return ProfileResponse.from(profile, resolveEmail(profile.getMemberId()), memberQueryPort.isAdmin(profile.getMemberId()));
    }

    /** 프로필 수정 (본인만). */
    @Transactional
    public ProfileResponse updateProfile(Long memberId, UpdateProfileRequest req) {
        Profile profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        // mentoringRole은 선택값 — null이면 기존 역할 유지. 값이 오면 PATCH /me/mentoring-role 과 동일 규칙 적용(우회 방지).
        if (req.mentoringRole() != null) {
            int currentYear = LocalDate.now().getYear();
            boolean isNewStudent = req.admissionYear() == currentYear;
            if (isNewStudent && req.mentoringRole() != MentoringRole.MENTEE) {
                throw new CustomException(ErrorCode.INVALID_MENTORING_ROLE);
            }
            profile.updateMentoringRole(req.mentoringRole());
        }

        String pref = resolvePreferred(req.preferredLanguage(), req.language());
        Language bucket = toBucket(pref);
        profile.updateProfile(
                req.name(),
                req.department(),
                req.nationality(),
                req.admissionYear(),
                bucket,
                pref,
                req.bio()
        );
        return ProfileResponse.from(profile, resolveEmail(profile.getMemberId()), memberQueryPort.isAdmin(profile.getMemberId()));
    }

    /** 프로필 이미지 업로드 및 URL 저장. 갱신된 전체 프로필을 반환(프론트 setProfile용). */
    @Transactional
    public ProfileResponse updateProfileImage(Long memberId, byte[] imageBytes, String contentType) {
        Profile profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        String url = s3Service.uploadProfileImage(memberId, imageBytes, contentType);
        profile.updateProfileImage(url);
        return ProfileResponse.from(profile, resolveEmail(profile.getMemberId()), memberQueryPort.isAdmin(profile.getMemberId()));
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
        return ProfileResponse.from(profile, resolveEmail(profile.getMemberId()), memberQueryPort.isAdmin(profile.getMemberId()));
    }

    /** 회원 이름 검색 (관리자 회원검색 화면용). 부분일치·대소문자 무시. */
    public Page<MemberSearchResponse> searchByName(String name, Pageable pageable) {
        return profileRepository.findByNameContainingIgnoreCase(name, pageable).map(MemberSearchResponse::from);
    }
}
