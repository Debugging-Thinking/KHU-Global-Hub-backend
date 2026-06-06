package com.khu.globalhub.coursereview.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khu.globalhub.coursereview.infrastructure.dto.SugangLectureRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 경희대 수강신청 시스템(sugang.khu.ac.kr) "종합시간표 조회"(무로그인) 스크래퍼.
 *
 * 흐름: ① coreLectureList 페이지 1회 호출로 세션쿠키(WMONID/SUGANGSESSIONID) 발급
 *      ② 같은 세션으로 학과별 lectListJson(JSON) 호출 → 강의 행 수집.
 * 대학/학과 코드는 /resources/data/data_{year}.js (공개)를 받아 파싱.
 *
 * 학교 서버 부담 최소화를 위해 국제캠 소속 대학의 학과만, 호출 간 짧은 간격을 둔다.
 */
@Slf4j
@Component
public class KhuSugangClient {

    private static final String BASE = "https://sugang.khu.ac.kr";
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final String REFERER = BASE + "/core?attribute=coreLectureList&lang=ko&loginYn=N";

    /** 국제캠퍼스 소속 학부 대학 코드 (data_*.js의 daehak cd). 응답 campus_nm="국제"로 한 번 더 거른다. */
    private static final java.util.Set<String> GLOBAL_COLLEGE_CODES = java.util.Set.of(
            "A04754", // 공과대학
            "A05091", // 전자정보대학
            "A07340", // 소프트웨어융합대학
            "A05346", // 응용과학대학
            "A05165", // 생명과학대학
            "A05366", // 국제대학
            "A04620", // 외국어대학
            "A05240", // 예술·디자인대학
            "A05297", // 체육대학
            "A11066", // 자유전공학부
            "A10258", // 후마니타스칼리지(국제)
            "A10158", // 융합
            "A05418", // 자연계열
            "A05522"  // 기타(국제)
    );

    private final ObjectMapper om = new ObjectMapper();

    /** 학과 참조 (조회 코드 + 소속 대학명). */
    public record MajorRef(String code, String college) {}

    /** 수집 결과 1행 (원본 행 + 소속 대학명). */
    public record CatalogRow(SugangLectureRow row, String college) {}

    /**
     * 국제캠 전체 학부 강의를 수집한다.
     * @param year 연도(예 2026) / @param termCode 학기코드(10=1학기,20=2학기,15/25=계절)
     */
    public List<CatalogRow> fetchGlobalCampus(int year, String termCode) {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        establishSession(client, year);
        List<MajorRef> majors = loadGlobalMajors(year, termCode);
        log.info("[Sugang] 국제캠 학과 {}개 수집 시작 ({}-{})", majors.size(), year, termCode);

        List<CatalogRow> out = new ArrayList<>();
        for (MajorRef m : majors) {
            try {
                for (SugangLectureRow r : fetchLectures(client, m.code(), year, termCode)) {
                    out.add(new CatalogRow(r, m.college()));
                }
                Thread.sleep(120); // 호출 간격 (서버 예의)
            } catch (Exception e) {
                log.warn("[Sugang] 학과 {} 수집 실패: {}", m.code(), e.getMessage());
            }
        }
        log.info("[Sugang] 수집 행 {}건", out.size());
        return out;
    }

    private void establishSession(HttpClient client, int year) {
        try {
            HttpRequest req = base(BASE + "/core?attribute=coreLectureList&lang=ko&loginYn=N&fake=" + System.currentTimeMillis())
                    .GET().build();
            client.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("[Sugang] 세션 수립 실패: {}", e.getMessage());
        }
    }

    private List<SugangLectureRow> fetchLectures(HttpClient client, String majorCode, int year, String termCode) throws Exception {
        String url = BASE + "/core?attribute=lectListJson&lang=ko&loginYn=N&menu=1&search_div=E"
                + "&p_day=&p_time=&p_teach=&p_subjt=&p_lang=&lecture_cd="
                + "&p_major=" + majorCode + "&p_year=" + year + "&p_term=" + termCode
                + "&initYn=Y&fake=" + System.currentTimeMillis();
        HttpRequest req = base(url).header("X-Requested-With", "XMLHttpRequest").GET().build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        if (res.statusCode() != 200) return List.of();
        String body = res.body();
        if (body == null || !body.contains("\"rows\"")) return List.of();
        JsonNode rows = om.readTree(body).path("rows");
        List<SugangLectureRow> list = new ArrayList<>();
        for (JsonNode n : rows) {
            list.add(om.treeToValue(n, SugangLectureRow.class));
        }
        return list;
    }

    private HttpRequest.Builder base(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", UA)
                .header("Referer", REFERER)
                .header("Accept", "application/json, text/javascript, */*; q=0.01");
    }

    /** data_{year}.js를 받아 국제캠 대학 소속 학과 목록을 만든다. */
    public List<MajorRef> loadGlobalMajors(int year, String termCode) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = base(BASE + "/resources/data/data_" + year + ".js?fake=" + System.currentTimeMillis()).GET().build();
            String js = client.send(req, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8)).body();

            Map<String, String> collegeName = parseRows(js, "daehak_" + year + termCode).stream()
                    .collect(LinkedHashMap::new, (mp, n) -> mp.put(n.path("cd").asText(), n.path("nm").asText()), Map::putAll);

            List<MajorRef> majors = new ArrayList<>();
            for (JsonNode n : parseRows(js, "major_" + year + termCode)) {
                String dh = n.path("dh").asText();
                if (!GLOBAL_COLLEGE_CODES.contains(dh)) continue;
                majors.add(new MajorRef(n.path("cd").asText(), collegeName.getOrDefault(dh, "")));
            }
            return majors;
        } catch (Exception e) {
            log.warn("[Sugang] 학과 코드표 로드 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /** `var {varName} = {"rows":[...]}` 에서 rows 배열을 추출해 JsonNode 리스트로 반환. */
    private List<JsonNode> parseRows(String js, String varName) throws Exception {
        Pattern p = Pattern.compile(Pattern.quote("var " + varName) + "\\s*=\\s*(\\{\"rows\":\\[[\\s\\S]*?\\]\\s*\\})");
        Matcher mt = p.matcher(js);
        if (!mt.find()) return List.of();
        JsonNode rows = om.readTree(mt.group(1)).path("rows");
        List<JsonNode> out = new ArrayList<>();
        rows.forEach(out::add);
        return out;
    }
}
