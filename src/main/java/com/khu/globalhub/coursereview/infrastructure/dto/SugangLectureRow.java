package com.khu.globalhub.coursereview.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 경희대 수강신청 종합시간표(lectListJson) 응답의 강의 1행.
 * 필드명은 서버 JSON 키와 동일(snake_case). 사용하지 않는 키는 무시.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SugangLectureRow(
        String subjt_name,       // 강좌명
        String teach_na,         // 교수명
        String unit_num,         // 학점 (예 "  3.0")
        Integer lect_grade,      // 대상학년
        Integer asign_pcnt,      // 정원
        String timetable,        // 강의시간/강의실 (HTML <BR> 포함)
        String field_gb,         // 이수구분 코드 (05=전공선택 등)
        String campus_nm,        // 캠퍼스 ("국제"/"서울")
        String lecture_cd,       // 학수번호+분반 (예 EE21002)
        String lecture_cd_disp,  // 표시용 학수번호 (예 EE210-02)
        String subjt_cd,         // 과목코드 (예 EE210)
        String haksu_code        // 사이버강좌 코드(없으면 "")
) {}
