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

    /** ?꾨줈???묐떟 議곕┰???대찓??議고쉶 (identity BC瑜?ID濡쒕쭔 李몄“). */
    private String resolveEmail(Long memberId) {
        return memberQueryPort.findEmail(memberId).orElse(null);
    }

    /**
     * 理쒖큹 ?꾨줈???앹꽦 (POST /api/auth/profile). identity??ProfileGateway媛 ?꾩엫.
     * - ?좎엯???낇븰?꾨룄==?꾩옱?꾨룄): MENTEE留?媛??
     * - ?ы븰?? MENTOR/MENTEE ?좏깮 (NONE? ?꾨줈???섏젙?먯꽌留?
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

    /** ?꾨줈??議고쉶 (蹂몄씤 ?먮뒗 ???. */
    public ProfileResponse getProfile(Long targetMemberId) {
        Profile profile = profileRepository.findByMemberId(targetMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));
        return ProfileResponse.from(profile, resolveEmail(profile.getMemberId()));
    }

    /** ?꾨줈???섏젙 (蹂몄씤留?. */
    @Transactional
    public ProfileResponse updateProfile(Long memberId, UpdateProfileRequest req) {
        Profile profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        // ?좎엯???낇븰?꾨룄 == ?꾩옱?꾨룄)? MENTEE 怨좎젙 ??PATCH /me/mentoring-role 怨??숈씪 洹쒖튃 (?고쉶 諛⑹?)
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
                req.language(),
                req.bio()
        );
        profile.updateMentoringRole(req.mentoringRole());
        return ProfileResponse.from(profile, resolveEmail(profile.getMemberId()));
    }

    /** ?꾨줈???대?吏 ?낅줈??諛?URL ??? */
    @Transactional
    public String updateProfileImage(Long memberId, byte[] imageBytes, String contentType) {
        Profile profile = profileRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));

        String url = s3Service.uploadProfileImage(memberId, imageBytes, contentType);
        profile.updateProfileImage(url);
        return url;
    }

    /**
     * 硫섑넗留???븷 蹂寃?(蹂몄씤留?.
     * ?좎엯???낇븰?꾨룄 == ?꾩옱?꾨룄)? MENTEE 怨좎젙 ??蹂寃?遺덇?.
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
