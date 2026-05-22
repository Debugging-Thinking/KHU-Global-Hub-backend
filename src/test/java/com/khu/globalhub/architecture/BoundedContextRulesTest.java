package com.khu.globalhub.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * BC(Bounded Context) 격리 경계 규칙 강제 (정적 분석, Spring 컨텍스트 불필요).
 * 각 BC는 ID 참조 / 통합 이벤트 / shared 포트로만 다른 BC와 통신해야 한다.
 *
 * 현재 허용된 잔여 결합 (의도적, 문서화됨):
 *  - board → qna : generic Comment가 qna 댓글을 섬김 → D3(qna 댓글 폐기)에서 제거 예정.
 *  - mentoring → profile : 매칭 알고리즘이 Profile(역할/입학년도) 직접 조회 → 추후 포트화.
 *  - shared.infra.TranslationService → board/qna : 번역 영속화가 BC 엔티티에 결합 → 추후 분리.
 *    (shared→BC는 아래 규칙 범위 밖. 별도 부채로 추적.)
 */
@DisplayName("[architecture] BC 격리 경계 규칙")
class BoundedContextRulesTest {

    private static final String ROOT = "com.khu.globalhub";
    private static JavaClasses classes;

    @BeforeAll
    static void importMainClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    private static String bc(String name) {
        return ROOT + "." + name + "..";
    }

    private void bcMustNotDependOn(String owner, String... forbiddenBcs) {
        String[] pkgs = new String[forbiddenBcs.length];
        for (int i = 0; i < forbiddenBcs.length; i++) {
            pkgs[i] = bc(forbiddenBcs[i]);
        }
        ArchRule rule = noClasses().that().resideInAPackage(bc(owner))
                .should().dependOnClassesThat().resideInAnyPackage(pkgs)
                .because(owner + "는 ID/이벤트/shared 포트로만 다른 BC와 통신해야 한다");
        rule.check(classes);
    }

    @Test
    @DisplayName("identity는 어떤 BC도 의존하지 않는다 (뿌리)")
    void identityIsRoot() {
        bcMustNotDependOn("identity", "profile", "board", "qna", "chat", "mentoring", "campusguide");
    }

    @Test
    @DisplayName("profile은 타 BC를 직접 의존하지 않는다 (shared 계약만)")
    void profileDependsOnlyOnShared() {
        bcMustNotDependOn("profile", "identity", "board", "qna", "chat", "mentoring", "campusguide");
    }

    @Test
    @DisplayName("qna는 어떤 BC도 의존하지 않는다")
    void qnaIsIsolated() {
        bcMustNotDependOn("qna", "identity", "profile", "board", "chat", "mentoring", "campusguide");
    }

    @Test
    @DisplayName("chat은 어떤 BC도 의존하지 않는다 (shared 포트/이벤트만)")
    void chatIsIsolated() {
        bcMustNotDependOn("chat", "identity", "profile", "board", "qna", "mentoring", "campusguide");
    }

    @Test
    @DisplayName("campusguide는 어떤 BC도 의존하지 않는다 (profile 갱신은 이벤트로)")
    void campusguideIsIsolated() {
        bcMustNotDependOn("campusguide", "identity", "profile", "board", "qna", "chat", "mentoring");
    }

    @Test
    @DisplayName("board는 qna 외 다른 BC를 의존하지 않는다 (board→qna는 generic Comment 잔여, D3 제거 예정)")
    void boardOnlyTouchesQna() {
        bcMustNotDependOn("board", "identity", "profile", "chat", "mentoring", "campusguide");
    }

    @Test
    @DisplayName("mentoring은 profile 외 다른 BC를 의존하지 않는다 (mentoring→profile은 매칭 데이터)")
    void mentoringOnlyTouchesProfile() {
        bcMustNotDependOn("mentoring", "identity", "board", "qna", "chat", "campusguide");
    }

    @Test
    @DisplayName("BC domain 계층은 application/infrastructure/presentation을 의존하지 않는다")
    void domainIsInnermostLayer() {
        ArchRule rule = noClasses().that().resideInAPackage(ROOT + "..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + "..application..", ROOT + "..infrastructure..", ROOT + "..presentation..")
                .because("domain은 최내곽 계층 — 상위 계층을 알지 못한다");
        rule.check(classes);
    }
}
