package com.khu.globalhub.profile.application;

import com.khu.globalhub.profile.infrastructure.ProfileRepository;
import com.khu.globalhub.shared.extevent.campusguide.QuizCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * campusguide의 {@link QuizCompletedEvent}를 받아 프로필 최고 점수를 갱신한다.
 * profile BC가 quizScore의 소유자 — campusguide는 profile 내부를 모른다.
 * AFTER_COMMIT 동기 실행 → 응시 트랜잭션 커밋 직후 같은 스레드에서 반영(동작 보존).
 */
@Component
@RequiredArgsConstructor
public class QuizCompletedListener {

    private final ProfileRepository profileRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onQuizCompleted(QuizCompletedEvent event) {
        profileRepository.findByMemberId(event.memberId()).ifPresent(profile -> {
            if (event.score() > profile.getQuizScore()) {
                profile.updateQuizScore(event.score());
            }
        });
    }
}
