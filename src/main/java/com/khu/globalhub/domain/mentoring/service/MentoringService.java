package com.khu.globalhub.domain.mentoring.service;

import com.khu.globalhub.domain.chat.entity.ChatMessage;
import com.khu.globalhub.domain.chat.repository.ChatMessageRepository;
import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.domain.member.repository.MemberRepository;
import com.khu.globalhub.domain.member.repository.ProfileRepository;
import com.khu.globalhub.domain.mentoring.dto.MentoringMatchResponse;
import com.khu.globalhub.domain.mentoring.entity.MentorMenteeMatch;
import com.khu.globalhub.domain.mentoring.repository.MentorMenteeMatchRepository;
import com.khu.globalhub.shared.enums.MatchStatus;
import com.khu.globalhub.shared.enums.MentoringRole;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentoringService {

    private final ProfileRepository profileRepository;
    private final MemberRepository memberRepository;
    private final MentorMenteeMatchRepository matchRepository;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 내 현재 ACTIVE 매칭 단건 조회.
     * 매칭 없으면 MATCH_NOT_FOUND 예외 발생 → 404 반환.
     */
    @Transactional(readOnly = true)
    public MentoringMatchResponse getMyMatch(Long memberId) {
        MentorMenteeMatch match = matchRepository.findActiveMatchesByMemberId(memberId, MatchStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));

        boolean isMentor = match.getMentorId().equals(memberId);
        Long partnerId = isMentor ? match.getMenteeId() : match.getMentorId();
        Profile partnerProfile = profileRepository.findByMemberId(partnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_NOT_FOUND));
        String partnerEmail = memberRepository.findById(partnerId)
                .map(m -> m.getEmail())
                .orElse(null);
        return MentoringMatchResponse.of(match, memberId, partnerProfile, partnerEmail);
    }

    /**
     * 매년 3월 1일: 작년 입학한 멘티를 자동으로 MENTOR 승격.
     * 스케줄러에서 호출된다.
     */
    @Transactional
    public void promoteOldMenteesToMentor() {
        int currentYear = LocalDate.now().getYear();
        List<Profile> targets = profileRepository
                .findByAdmissionYearLessThanAndMentoringRole(currentYear, MentoringRole.MENTEE);

        for (Profile profile : targets) {
            profile.updateMentoringRole(MentoringRole.MENTOR);
        }
        log.info("[Mentoring] Promoted {} mentees to mentor (year < {})", targets.size(), currentYear);
    }

    /**
     * 멘토-멘티 자동 매칭.
     * 스케줄러에서 호출된다.
     *
     * 알고리즘 (순환 배정):
     * - max(멘토수, 멘티수)번 반복
     * - 작은 쪽을 % 나머지로 순환 → 빈 매칭 없이 균등 분배
     *
     * 예) 멘토 5명, 멘티 3명:
     *   i=0: 멘토0 ↔ 멘티0
     *   i=1: 멘토1 ↔ 멘티1
     *   i=2: 멘토2 ↔ 멘티2
     *   i=3: 멘토3 ↔ 멘티0 (순환)
     *   i=4: 멘토4 ↔ 멘티1 (순환)
     */
    @Transactional
    public void runMatching(String semester) {
        List<Profile> allMentees = profileRepository.findByMentoringRole(MentoringRole.MENTEE);
        List<Profile> unmatchedMentees = allMentees.stream()
                .filter(p -> !matchRepository.existsByMenteeIdAndSemester(p.getMemberId(), semester))
                .toList();

        List<Profile> mentors = profileRepository.findByMentoringRole(MentoringRole.MENTOR);

        if (mentors.isEmpty() || unmatchedMentees.isEmpty()) {
            log.info("[Mentoring] Matching skipped: mentors={}, mentees={}", mentors.size(), unmatchedMentees.size());
            return;
        }

        int total = Math.max(mentors.size(), unmatchedMentees.size());
        for (int i = 0; i < total; i++) {
            Profile mentor = mentors.get(i % mentors.size());
            Profile mentee = unmatchedMentees.get(i % unmatchedMentees.size());
            createMatch(mentor, mentee, semester);
        }

        log.info("[Mentoring] Matching complete for semester {}: {} pairs created", semester, total);
    }

    private void createMatch(Profile mentorProfile, Profile menteeProfile, String semester) {
        MentorMenteeMatch match = MentorMenteeMatch.builder()
                .mentorId(mentorProfile.getMemberId())
                .menteeId(menteeProfile.getMemberId())
                .semester(semester)
                .build();
        matchRepository.save(match);

        // 멘토에게 시스템 메시지 삽입
        chatMessageRepository.save(ChatMessage.builder()
                .receiverId(mentorProfile.getMemberId())
                .contextPartnerId(menteeProfile.getMemberId())
                .content(buildSystemMessage(mentorProfile.getName(), menteeProfile.getName()))
                .isSystem(true)
                .build());

        // 멘티에게도 동일 시스템 메시지 삽입
        chatMessageRepository.save(ChatMessage.builder()
                .receiverId(menteeProfile.getMemberId())
                .contextPartnerId(mentorProfile.getMemberId())
                .content(buildSystemMessage(mentorProfile.getName(), menteeProfile.getName()))
                .isSystem(true)
                .build());
    }

    private String buildSystemMessage(String mentorName, String menteeName) {
        return String.format(
                "멘토-멘티 매칭이 완료되었습니다!\n멘토: %s / 멘티: %s\n자유롭게 대화를 시작해보세요.",
                mentorName, menteeName);
    }
}
