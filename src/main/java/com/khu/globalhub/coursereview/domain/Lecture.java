package com.khu.globalhub.coursereview.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 강의(과목) 엔티티 — 강의평의 대상. (경희대 국제캠퍼스 수강편람 기준)
 * MVP: 학수번호/과목명/교수/단과대/이수구분/학점/학기.
 */
@Entity
@Table(name = "lectures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 학수번호 (예: CSE301) */
    @Column(nullable = false)
    private String code;

    /** 과목명 */
    @Column(nullable = false)
    private String name;

    /** 교수명 */
    @Column(nullable = false)
    private String professor;

    /** 단과대학/개설 */
    private String college;

    /** 이수구분 (전공/교양 등) */
    private String type;

    /** 학점 */
    private Integer credits;

    /** 학기 (예: 2026-1) */
    @Column(nullable = false)
    private String semester;

    /** 수강편람 재수집 시 카탈로그 정보 갱신 (학수번호/학기는 식별자라 불변). */
    public void updateCatalog(String name, String professor, String college, String type, Integer credits) {
        this.name = name;
        this.professor = professor;
        this.college = college;
        this.type = type;
        this.credits = credits;
    }
}
