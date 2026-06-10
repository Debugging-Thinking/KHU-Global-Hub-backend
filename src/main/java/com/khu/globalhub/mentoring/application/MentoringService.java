package com.khu.globalhub.mentoring.application;

import com.khu.globalhub.profile.domain.Profile;
import com.khu.globalhub.shared.port.MemberQueryPort;
import com.khu.globalhub.profile.infrastructure.ProfileRepository;
import com.khu.globalhub.mentoring.presentation.dto.MentoringMatchResponse;
import com.khu.globalhub.mentoring.presentation.dto.AdminMatchResponse;
import com.khu.globalhub.mentoring.domain.MentorMenteeMatch;
import com.khu.globalhub.mentoring.infrastructure.MentorMenteeMatchRepository;
import com.khu.globalhub.shared.extevent.mentoring.MatchCreatedEvent;
import com.khu.globalhub.shared.enums.MatchStatus;
import com.khu.globalhub.shared.enums.MentoringRole;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentoringService {

    private final ProfileRepository profileRepository;
    private final MemberQueryPort memberQueryPort;
    private final MentorMenteeMatchRepository matchRepository;
    private final ApplicationEventPublisher eventPublisher;

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
        String partnerEmail = memberQueryPort.findEmail(partnerId).orElse(null);
        return MentoringMatchResponse.of(match, memberId, partnerProfile, partnerEmail);
    }

    @Transactional(readOnly = true)
    public List<MentoringMatchResponse> getMyMatchHistory(Long memberId) {
        List<MentorMenteeMatch> matches = matchRepository.findAllMatchesByMemberId(memberId);
        return matches.stream().map(match -> {
            boolean isMentor = match.getMentorId().equals(memberId);
            Long partnerId = isMentor ? match.getMenteeId() : match.getMentorId();
            Profile partnerProfile = profileRepository.findByMemberId(partnerId).orElse(null);
            if (partnerProfile == null) return null;
            String partnerEmail = memberQueryPort.findEmail(partnerId).orElse(null);
            return MentoringMatchResponse.of(match, memberId, partnerProfile, partnerEmail);
        }).filter(java.util.Objects::nonNull).toList();
    }

    /** 관리자 전체 매칭 현황 (멘토/멘티 이름 포함, 탈퇴 시 "(탈퇴)" 폴백). */
    @Transactional(readOnly = true)
    public List<AdminMatchResponse> getAllMatches() {
        return matchRepository.findAll().stream()
                .map(m -> new AdminMatchResponse(
                        m.getId(),
                        m.getMentorId(),
                        profileRepository.findByMemberId(m.getMentorId()).map(Profile::getName).orElse("(탈퇴)"),
                        m.getMenteeId(),
                        profileRepository.findByMemberId(m.getMenteeId()).map(Profile::getName).orElse("(탈퇴)"),
                        m.getSemester(),
                        m.getStatus().name(),
                        m.getMatchedAt()))
                .toList();
    }

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
     * Score-based greedy matching + round-robin for remaining members.
     *
     * Scoring:
     *   Same nationality : +3
     *   Same language    : +2
     *   1~2 year senior  : +1
     *
     * Step 1: Calculate scores for all mentor-mentee pairs, sort descending.
     * Step 2: Greedy 1:1 matching by highest score.
     * Step 3: Round-robin assignment for remaining unmatched members.
     */
    @Transactional
    public void runMatching(String semester) {
        List<Profile> unmatchedMentees = profileRepository.findByMentoringRole(MentoringRole.MENTEE)
                .stream()
                .filter(p -> !matchRepository.existsByMenteeIdAndSemester(p.getMemberId(), semester))
                .filter(p -> !memberQueryPort.isAdmin(p.getMemberId()))   // 관리자는 매칭 풀에서 제외
                .collect(Collectors.toCollection(ArrayList::new));

        List<Profile> unmatchedMentors = profileRepository.findByMentoringRole(MentoringRole.MENTOR)
                .stream()
                .filter(p -> !matchRepository.existsByMentorIdAndSemester(p.getMemberId(), semester))
                .filter(p -> !memberQueryPort.isAdmin(p.getMemberId()))   // 관리자는 매칭 풀에서 제외
                .collect(Collectors.toCollection(ArrayList::new));

        if (unmatchedMentors.isEmpty() || unmatchedMentees.isEmpty()) {
            log.info("[Mentoring] Matching skipped: mentors={}, mentees={}", unmatchedMentors.size(), unmatchedMentees.size());
            return;
        }

        record Candidate(Profile mentor, Profile mentee, int score) {}
        List<Candidate> candidates = new ArrayList<>();
        for (Profile mentor : unmatchedMentors) {
            for (Profile mentee : unmatchedMentees) {
                candidates.add(new Candidate(mentor, mentee, calcScore(mentor, mentee)));
            }
        }
        candidates.sort((a, b) -> b.score() - a.score());

        Set<Long> matchedMentorIds = new HashSet<>();
        Set<Long> matchedMenteeIds = new HashSet<>();
        List<Profile> pairedMentors = new ArrayList<>();
        List<Profile> pairedMentees = new ArrayList<>();
        int pairCount = 0;

        for (Candidate c : candidates) {
            Long mentorId = c.mentor().getMemberId();
            Long menteeId = c.mentee().getMemberId();
            if (matchedMentorIds.contains(mentorId) || matchedMenteeIds.contains(menteeId)) continue;
            createMatch(c.mentor(), c.mentee(), semester);
            matchedMentorIds.add(mentorId);
            matchedMenteeIds.add(menteeId);
            pairedMentors.add(c.mentor());
            pairedMentees.add(c.mentee());
            pairCount++;
        }

        List<Profile> remainingMentees = unmatchedMentees.stream()
                .filter(p -> !matchedMenteeIds.contains(p.getMemberId()))
                .toList();
        for (int i = 0; i < remainingMentees.size(); i++) {
            createMatch(pairedMentors.get(i % pairedMentors.size()), remainingMentees.get(i), semester);
            pairCount++;
        }

        List<Profile> remainingMentors = unmatchedMentors.stream()
                .filter(p -> !matchedMentorIds.contains(p.getMemberId()))
                .toList();
        for (int i = 0; i < remainingMentors.size(); i++) {
            createMatch(remainingMentors.get(i), pairedMentees.get(i % pairedMentees.size()), semester);
            pairCount++;
        }

        log.info("[Mentoring] Matching complete for semester {}: {} pairs created", semester, pairCount);
    }

    private int calcScore(Profile mentor, Profile mentee) {
        int score = 0;
        if (mentor.getNationality() != null && mentor.getNationality().equalsIgnoreCase(mentee.getNationality())) {
            score += 3;
        }
        if (mentor.getLanguage() != null && mentor.getLanguage() == mentee.getLanguage()) {
            score += 2;
        }
        int yearDiff = mentor.getAdmissionYear() - mentee.getAdmissionYear();
        if (yearDiff >= -2 && yearDiff <= -1) {
            score += 1;
        }
        return score;
    }

    private void createMatch(Profile mentorProfile, Profile menteeProfile, String semester) {
        MentorMenteeMatch match = MentorMenteeMatch.builder()
                .mentorId(mentorProfile.getMemberId())
                .menteeId(menteeProfile.getMemberId())
                .semester(semester)
                .build();
        matchRepository.save(match);
        eventPublisher.publishEvent(new MatchCreatedEvent(
                mentorProfile.getMemberId(), menteeProfile.getMemberId()));
    }
}