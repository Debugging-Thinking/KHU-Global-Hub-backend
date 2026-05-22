package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.domain.QuizQuestion;
import com.khu.globalhub.campusguide.infrastructure.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class QuizDataInitializer implements ApplicationRunner {

    private final QuizQuestionRepository questionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (questionRepository.count() > 0) return;

        List<QuizQuestion> questions = List.of(
            build("수강신청",
                "신입생이 수강신청 시 PC로만 신청 가능한 이유는?",
                List.of("앱에 오류가 자주 발생해서",
                        "희망과목 담기 / 예비과목 담기를 사전에 진행하지 않았기 때문",
                        "학교 규정상 신입생은 무조건 PC 사용",
                        "모바일 앱은 2학년 이상만 사용 가능"),
                1, "신입생은 사전 희망과목 담기/예비과목 담기를 진행하지 않았기 때문에 PC 웹사이트로만 수강신청이 가능합니다."),

            build("수강신청",
                "수강신청 시 정각 타이밍 확인에 권장되는 서버시간 도구는?",
                List.of("네이버 시계", "카카오 시계", "네이비즘", "윈도우 내장 시계"),
                2, "네이비즘을 사용하고 밀리초 ON / 날짜 OFF 설정을 권장합니다. 0.001초 차이가 수백~수천 명의 순위 차이를 만들 수 있습니다."),

            build("수강신청",
                "수강신청 도중 절대 눌러서는 안 되는 키는?",
                List.of("Enter", "Space", "Tab", "F5"),
                3, "F5(새로고침)는 수강신청 중 절대 사용하면 안 됩니다. ESC를 약 1,000회 연타하면 수강신청 내역 전체가 초기화됩니다."),

            build("수강신청",
                "수강신청 당일, 신청 완료 확인창이 뜨면 어떤 키로 닫아야 하나요?",
                List.of("Enter", "ESC", "Delete", "Backspace"),
                1, "확인창이 뜨면 ESC 키로 닫고 다음 학수번호를 바로 신청해야 합니다. 단, ESC 연타는 매크로 방지 팝업을 유발할 수 있으니 주의하세요."),

            build("수강신청",
                "취소지연제는 몇 시경부터 적용되나요?",
                List.of("10시", "11시", "11시 30분", "12시"),
                2, "취소지연제는 11시 30분경부터 적용됩니다. 다른 학생이 수강을 취소해도 일정 시간이 지난 후에야 신청 가능합니다."),

            build("수강신청",
                "학점세이브제를 활용하면 다음 학기 최대 몇 학점까지 신청 가능한가요?",
                List.of("18학점", "19학점", "20학점", "21학점"),
                3, "학점세이브제는 F학점이 없는 경우 최대 3학점 세이브 가능하며, 다음 학기에 최대 21학점 신청이 가능합니다."),

            build("교통수단",
                "경희대 교내 구간 버스를 무료로 타는 방법은?",
                List.of("학생증을 기사님께 보여준다",
                        "카드를 찍지 않으면 된다",
                        "앱에서 무료 탑승 쿠폰을 발급받는다",
                        "기사님께 학생임을 말한다"),
                1, "교내 구간에서는 카드를 찍지 않으면 무료입니다. 탑승 시 기사님께 '감사합니다~' 인사는 꼭 해주세요!"),

            build("교통수단",
                "1550-1 버스에서 강남 방면으로 탑승하는 정류장은?",
                List.of("정건 (경희대학교 정류장)", "영통역 6번 출구", "정문 (경희대정문 정류장)", "학생회관 앞"),
                2, "1550-1은 방향 혼동이 잦은 버스입니다. 강남 방면은 정문(경희대정문 정류장), 한신대 방면은 정건(경희대학교 정류장)에서 탑승해야 합니다."),

            build("교통수단",
                "G5100 버스의 특징은?",
                List.of("교내 무료 구간이 없는 버스", "2층 버스", "M버스 (광역급행버스)", "경희대 기점이 아닌 버스"),
                1, "G5100은 강남행 2층 버스입니다. 직행좌석버스 노선 중 유일한 2층 버스입니다."),

            build("교통수단",
                "영통역에서 경희대 정문까지 도보로 약 몇 분 거리인가요?",
                List.of("5분", "10분", "15분", "20분"),
                2, "영통역에서 학교까지 도보 약 15분 거리입니다. 전정대·국제대·중앙도서관 등 사색의 광장 쪽 건물이 목적지라면 버스 이용을 권장합니다."),

            build("후마니타스 교양",
                "2024학년도 입학생 기준 필수교과 이수학점은 총 몇 학점인가요?",
                List.of("12학점", "15학점", "17학점", "20학점"),
                2, "필수교과는 인간의가치탐색(3), 세계와시민(3), 빅뱅에서문명까지(3), 성찰과표현(3), 주제연구(3), 대학영어(2) 총 17학점입니다."),

            build("후마니타스 교양",
                "글쓰기 필수과목 '주제연구'를 수강하기 위한 선수과목은?",
                List.of("세계와시민", "인간의가치탐색", "성찰과표현", "대학영어"),
                2, "'성찰과표현'은 1학년 필수 과목이자 '주제연구'의 선수과목입니다. 1학년 때 반드시 이수해야 합니다."),

            build("학교 사이트",
                "수강신청, 성적 조회, 학사 일정 확인을 모두 할 수 있는 사이트는?",
                List.of("e-Campus (이캠퍼스)", "인포21 (Info21)", "경희대 공식 홈페이지", "에브리타임"),
                1, "인포21(info21.khu.ac.kr)은 수강신청, 성적 조회, 학사 일정 등 학사 전반을 관리하는 핵심 사이트입니다."),

            build("학교 사이트",
                "그룹 스터디룸 온라인 예약이 가능한 사이트는?",
                List.of("인포21", "lib.khu.ac.kr (중앙도서관)", "khu-kr.libcal.com", "khucoop.com (생협)"),
                2, "스터디룸 예약은 khu-kr.libcal.com에서 온라인으로 가능합니다."),

            build("맛집",
                "영통 유일의 텐동 전문점으로 알려진 식당은?",
                List.of("겐코", "부타센세", "쿠지라", "미미카츠"),
                2, "쿠지라는 영통 유일의 텐동 전문점입니다. 가게 수용 인원이 적으니 방문 시 참고하세요."),

            build("맛집",
                "밥 무한리필이 가능한 영통 식당은?",
                List.of("천보현", "부대통령", "좌우지간", "사담손만두"),
                1, "부대통령은 밥 무한리필이 가능합니다. 배가 많이 고픈 날 찌개 하나에 밥 두 공기도 거뜬하다는 후기가 있습니다."),

            build("맛집",
                "천보현의 점심특선(6,900원) 메뉴 조합은?",
                List.of("제육볶음 + 된장찌개", "육회비빔밥 + 차돌된장찌개", "비빔밥 + 순두부찌개", "냉면 + 만두"),
                1, "천보현의 6,900원 점심특선은 육회비빔밥 + 차돌된장찌개 조합으로, 유명 고깃집보다 낫다는 평가도 있습니다."),

            build("맛집",
                "영통 마라탕 맛집으로 정평이 난 중식당은?",
                List.of("짜마차이나", "얜시부", "겐코", "좌우지간"),
                1, "얜시부는 마라탕 맛집으로 정평이 나 있으며, 마라탕 + 계란볶음밥 조합이 특히 인기입니다.")
        );

        questionRepository.saveAll(questions);
    }

    private QuizQuestion build(String category, String question, List<String> options,
                                int answerIndex, String explanation) {
        return QuizQuestion.builder()
                .category(category)
                .question(question)
                .options(options)
                .answerIndex(answerIndex)
                .explanation(explanation)
                .build();
    }
}
