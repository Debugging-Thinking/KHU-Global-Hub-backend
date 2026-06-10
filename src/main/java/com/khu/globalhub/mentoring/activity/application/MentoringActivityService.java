package com.khu.globalhub.mentoring.activity.application;

import com.khu.globalhub.mentoring.activity.domain.ActivityImage;
import com.khu.globalhub.mentoring.activity.domain.MentoringActivity;
import com.khu.globalhub.mentoring.activity.infrastructure.ActivityImageRepository;
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

    private static final int MAX_IMAGES = 5;

    private final MentoringActivityRepository activityRepository;
    private final MentorMenteeMatchRepository matchRepository;
    private final ActivityImageRepository activityImageRepository;

    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivities(Long matchId, Long requesterId) {
        MentorMenteeMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));
        if (!match.getMentorId().equals(requesterId) && !match.getMenteeId().equals(requesterId)) {
            throw new CustomException(ErrorCode.ACTIVITY_UNAUTHORIZED);
        }
        return activityRepository.findByMatchIdOrderByCreatedAtAsc(matchId)
                .stream()
                .map(ActivityResponse::from)
                .toList();
    }

    @Transactional
    public ActivityResponse createActivity(Long matchId, Long authorId, CreateActivityRequest req) {
        MentorMenteeMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));
        if (!match.getMentorId().equals(authorId) && !match.getMenteeId().equals(authorId)) {
            throw new CustomException(ErrorCode.ACTIVITY_UNAUTHORIZED);
        }

        MentoringActivity activity = activityRepository.save(
                MentoringActivity.builder()
                        .matchId(matchId)
                        .authorId(authorId)
                        .title(req.title())
                        .content(req.content())
                        .build()
        );

        // Base64 이미지 저장 (최대 MAX_IMAGES장)
        if (req.imageDataUris() != null && !req.imageDataUris().isEmpty()) {
            int limit = Math.min(req.imageDataUris().size(), MAX_IMAGES);
            for (int i = 0; i < limit; i++) {
                String dataUri = req.imageDataUris().get(i);
                if (dataUri == null || dataUri.isBlank()) continue;
                activityImageRepository.save(ActivityImage.builder()
                        .activity(activity)
                        .imageUrl(dataUri)
                        .orderIndex(i)
                        .build());
            }
        }

        MentoringActivity saved = activityRepository.findById(activity.getId()).orElseThrow();
        return ActivityResponse.from(saved);
    }
}