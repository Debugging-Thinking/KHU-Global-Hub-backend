package com.khu.globalhub.mentoring.application;

import com.khu.globalhub.mentoring.application.MentoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 멘토링 관련 스케줄러.
 *
 * 매년 3월 1일 자정:
 *   1) 전년도 입학 멘티 → MENTOR 자동 승격
 *   2) 새 학기(1학기) 멘토-멘티 자동 매칭
 *
 * 매년 9월 1일 자정:
 *   2학기 멘토-멘티 자동 매칭
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mentoring.scheduler.enabled", havingValue = "true")
@RequiredArgsConstructor
public class MentoringScheduler {

    private final MentoringService mentoringService;

    /**
     * 매년 3월 1일 00:00 — 1학기 처리.
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 0 1 3 *")
    public void runFirstSemester() {
        int year = LocalDate.now().getYear();
        String semester = year + "-1";
        log.info("[Scheduler] First semester mentoring started: {}", semester);

        mentoringService.promoteOldMenteesToMentor();
        mentoringService.runMatching(semester);
    }

    /**
     * 매년 9월 1일 00:00 — 2학기 매칭.
     */
    @Scheduled(cron = "0 0 0 1 9 *")
    public void runSecondSemester() {
        int year = LocalDate.now().getYear();
        String semester = year + "-2";
        log.info("[Scheduler] Second semester mentoring started: {}", semester);

        mentoringService.runMatching(semester);
    }
}
