package com.khu.globalhub;

import org.junit.jupiter.api.Test;

/**
 * 스모크 테스트: 전체 Spring 컨텍스트가 실 PostgreSQL 위에서 정상 부팅되는지 확인.
 * 통과하면 (1) 테스트 하네스 동작, (2) 현재 엔티티로 스키마 생성 성공이 검증된다.
 */
class ContextLoadsTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // 컨텍스트 로딩만으로 검증 완료
    }
}
