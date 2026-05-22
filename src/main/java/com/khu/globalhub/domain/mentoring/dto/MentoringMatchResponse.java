package com.khu.globalhub.domain.mentoring.dto;

import com.khu.globalhub.domain.member.dto.ProfileResponse;
import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.domain.mentoring.entity.MentorMenteeMatch;
import com.khu.globalhub.shared.enums.MatchStatus;

import java.time.LocalDateTime;

public record MentoringMatchResponse(
        Long matchId,
        String semester,
        MatchStatus status,
        String myRole,           // "MENTOR" or "MENTEE"
        LocalDateTime matchedAt,
        ProfileResponse partner  // 상대방 프로필
) {
    public static MentoringMatchResponse of(MentorMenteeMatch match,
                                            Long myMemberId,
                                            Profile partnerProfile) {
        boolean isMentor = match.getMentor().getId().equals(myMemberId);
        return new MentoringMatchResponse(
                match.getId(),
                match.getSemester(),
                match.getStatus(),
                isMentor ? "MENTOR" : "MENTEE",
                match.getMatchedAt(),
                ProfileResponse.from(partnerProfile)
        );
    }
}
