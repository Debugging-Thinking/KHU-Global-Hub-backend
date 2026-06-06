package com.khu.globalhub.coursereview.application;

import com.khu.globalhub.coursereview.domain.Lecture;
import com.khu.globalhub.coursereview.infrastructure.KhuSugangClient;
import com.khu.globalhub.coursereview.infrastructure.KhuSugangClient.CatalogRow;
import com.khu.globalhub.coursereview.infrastructure.LectureRepository;
import com.khu.globalhub.coursereview.infrastructure.dto.SugangLectureRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 경희대 국제캠 수강편람을 수집해 lectures 테이블에 upsert한다.
 * 학수번호+학기를 식별자로 갱신하므로 재실행해도 강의평(FK)은 보존된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LectureImportService {

    private final KhuSugangClient sugangClient;
    private final LectureRepository lectureRepository;

    /** 이수구분 코드 → 한글 라벨 (종합시간표 범례 기준). */
    private static final Map<String, String> FIELD_GB = Map.ofEntries(
            Map.entry("11", "전공기초"), Map.entry("04", "전공필수"), Map.entry("05", "전공선택"),
            Map.entry("06", "교직과"), Map.entry("14", "중핵교과"), Map.entry("15", "배분이수교과"),
            Map.entry("16", "기초교과"), Map.entry("17", "자유이수"), Map.entry("20", "교직전선"),
            Map.entry("24", "계절학기전공필수"), Map.entry("27", "계절학기전공기초"), Map.entry("08", "일반선택"),
            Map.entry("10", "공통과목"), Map.entry("13", "공통필수")
    );

    /** HTTP 수집은 트랜잭션 밖에서 수행하고, upsert는 행별 save(=merge)로 처리한다. */
    public int importGlobalCampus(int year, String termCode) {
        List<CatalogRow> rows = sugangClient.fetchGlobalCampus(year, termCode);
        String semester = year + "-" + termLabel(termCode);

        // 같은 강의가 여러 학과에 노출될 수 있어 학수번호로 중복 제거(처음 것 유지).
        Map<String, CatalogRow> unique = new LinkedHashMap<>();
        for (CatalogRow cr : rows) {
            SugangLectureRow r = cr.row();
            if (r.campus_nm() == null || !r.campus_nm().contains("국제")) continue;
            if (r.subjt_name() == null || r.subjt_name().isBlank()) continue;
            String code = code(r);
            if (code.isBlank()) continue;
            unique.putIfAbsent(code, cr);
        }

        int count = 0;
        for (Map.Entry<String, CatalogRow> e : unique.entrySet()) {
            String code = e.getKey();
            SugangLectureRow r = e.getValue().row();
            String name = r.subjt_name().trim();
            String professor = (r.teach_na() == null || r.teach_na().isBlank()) ? "미정" : r.teach_na().trim();
            String college = e.getValue().college();
            String type = FIELD_GB.getOrDefault(r.field_gb(), r.field_gb());
            Integer credits = parseCredits(r.unit_num());

            lectureRepository.findByCodeAndSemester(code, semester)
                    .ifPresentOrElse(
                            l -> { l.updateCatalog(name, professor, college, type, credits); lectureRepository.save(l); },
                            () -> lectureRepository.save(Lecture.builder()
                                    .code(code).name(name).professor(professor)
                                    .college(college).type(type).credits(credits)
                                    .semester(semester).build()));
            count++;
        }
        log.info("[LectureImport] {} 강의 {}건 upsert 완료", semester, count);
        return count;
    }

    private String code(SugangLectureRow r) {
        if (r.lecture_cd_disp() != null && !r.lecture_cd_disp().isBlank()) return r.lecture_cd_disp().trim();
        return r.lecture_cd() == null ? "" : r.lecture_cd().trim();
    }

    private Integer parseCredits(String unitNum) {
        if (unitNum == null || unitNum.isBlank()) return null;
        try {
            return (int) Math.round(Double.parseDouble(unitNum.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String termLabel(String termCode) {
        return switch (termCode) {
            case "10" -> "1";
            case "20" -> "2";
            case "15" -> "여름";
            case "25" -> "겨울";
            default -> termCode;
        };
    }
}
