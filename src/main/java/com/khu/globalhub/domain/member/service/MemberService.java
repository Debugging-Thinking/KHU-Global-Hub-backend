package com.khu.globalhub.domain.member.service;

import com.khu.globalhub.domain.board.dto.PostSummaryResponse;
import com.khu.globalhub.domain.board.entity.PostTranslation;
import com.khu.globalhub.domain.board.repository.PostRepository;
import com.khu.globalhub.domain.board.repository.PostTranslationRepository;
import com.khu.globalhub.domain.member.dto.ProfileResponse;
import com.khu.globalhub.domain.member.dto.UpdateMentoringRoleRequest;
import com.khu.globalhub.domain.member.dto.UpdateProfileRequest;
import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.domain.member.repository.ProfileRepository;
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
    private final PostRepository postRepository;
    private final PostTranslationRepository postTranslationRepository;
    private final S3Service s3Service;

    /** 프로필 조회 (본인 또는 타인). */
    public ProfileResponse getProfile(Long targetMemberId) {
        Profile profile = profileRepository.findByMemberId(targetMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));
        return ProfileResponse.from(profile);
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
        return ProfileResponse.from(profile);
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
        return ProfileResponse.from(profile);
    }

    /**
     * 특정 멤버의 게시글 목록.
     * - 본인(myId == targetMemberId): 익명 게시글 포함
     * - 타인: 익명 게시글 제외
     */
    public Page<PostSummaryResponse> getMemberPosts(Long myId, Long targetMemberId,
                                                    Language language, Pageable pageable) {
        boolean isOwner = myId.equals(targetMemberId);

        var posts = isOwner
                ? postRepository.findByAuthorIdOrderByCreatedAtDesc(targetMemberId, pageable)
                : postRepository.findByAuthorIdAndIsAnonymousFalseOrderByCreatedAtDesc(targetMemberId, pageable);

        return posts.map(post -> {
            PostTranslation translation = postTranslationRepository
                    .findByPostIdAndLanguage(post.getId(), language)
                    .orElseGet(() -> postTranslationRepository
                            .findByPostIdAndLanguage(post.getId(), Language.EN)
                            .orElseGet(() -> post.getTranslations().get(0)));

            Profile authorProfile = profileRepository.findByMemberId(post.getAuthor().getId())
                    .orElse(null);
            String authorName = authorProfile != null ? authorProfile.getName() : "Unknown";

            return PostSummaryResponse.of(post, translation, authorName);
        });
    }
}
