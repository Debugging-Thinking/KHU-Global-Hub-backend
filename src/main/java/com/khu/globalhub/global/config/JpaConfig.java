package com.khu.globalhub.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화.
 * 이게 없으면 BaseTimeEntity의 @CreatedDate, @LastModifiedDate가 동작하지 않아
 * created_at / updated_at 이 null로 저장된다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
