package com.khu.globalhub.mentoring.activity.application;

import com.khu.globalhub.mentoring.activity.domain.MentoringActivity;
import com.khu.globalhub.mentoring.activity.infrastructure.MentoringActivityRepository;
import com.khu.globalhub.mentoring.activity.presentation.dto.ActivityResponse;
import com.khu.globalhub.mentoring.activity.presentation.dto.CreateActivityRequest;
import com.khu.globalhub.mentoring.domain.MentorMenteeMatch;
import com.khu.globalhub.mentoring.infrastructure.MentorMenteeMatchRepository;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MentoringActivityService {

    private final MentoringActivityRepository activityRepository;
    private final MentorMenteeMatchRepository matchRepository;

    /** 특정 매칭의 활동 기록 목록 조회 (오름차순) */
    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivities(Long matchId, Long requesterId) {
        MentorMenteeMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));

        // 매칭 참여자만 조회 가능
        if (!match.getMentorId().equals(requesterId) && !match.getMenteeId().equals(requesterId)) {
            throw new CustomException(ErrorCode.ACTIVITY_UNAUTHORIZED);
        }

        return activityRepository.findByMatchIdOrderByCreatedAtAsc(matchId)
                .stream()
                .map(ActivityResponse::from)
                .toList();
    }

    /** 활동 기록 작성 */
    @Transactional
    public ActivityResponse createActivity(Long matchId, Long authorId, CreateActivityRequest req) {
        MentorMenteeMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));

        // 매칭 참여자만 작성 가능
        if (!match.getMentorId().equals(authorId) && !match.getMenteeId().equals(authorId)) {
            throw new CustomException(ErrorCode.ACTIVITY_UNAUTHORIZED);
        }

        MentoringActivity activity = MentoringActivity.builder()
                .matchId(matchId)
                .authorId(authorId)
                .title(req.title())
                .content(req.content())
                .build();

        return ActivityResponse.from(activityRepository.save(activity));
    }
}