package com.khu.globalhub.campusguide.domain;

import com.khu.globalhub.shared.enums.Language;
import jakarta.persistence.*;
import lombok.*;

/**
 * 퀴즈 문항 번역 엔티티 — 게시판/강의평과 동일한 사전번역 방식.
 * 문항 1개당 최대 6개 행(언어별 1개). 작성/수정 시 원문 행 동기 저장 + 나머지 비동기 번역.
 *
 * options는 보기 여러 개(List&lt;String&gt;)라 JSON 문자열로 직렬화해 한 컬럼(options)에 저장한다.
 * (직렬화/역직렬화는 application 계층의 QuizOptionsCodec이 담당 — 도메인은 원시 문자열만 보관)
 */
@Entity
@Table(
    name = "quiz_question_translations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "language"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuizQuestionTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 문항의 번역인지 (ID 참조). */
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** 이 행이 어떤 언어 버전인지. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    /** 해당 언어로 번역된 질문. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    /** 보기 리스트를 JSON 문자열로 직렬화한 값 (예: ["a","b","c"]). */
    @Column(name = "options", columnDefinition = "TEXT", nullable = false)
    private String optionsJson;

    /** 해당 언어로 번역된 해설 (선택). */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    public void updateContent(String question, String optionsJson, String explanation) {
        this.question = question;
        this.optionsJson = optionsJson;
        this.explanation = explanation;
    }

    /** 원문 행을 자동 감지된 언어로 재라벨 (작성 시 claimed 언어와 다를 때). */
    public void updateLanguage(Language language) {
        this.language = language;
    }
}
