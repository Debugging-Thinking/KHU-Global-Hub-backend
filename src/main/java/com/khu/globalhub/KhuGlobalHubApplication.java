package com.khu.globalhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing       // createdAt, updatedAt 자동 기록 활성화
@EnableScheduling        // 멘토-멘티 자동 매칭 스케줄러 활성화
public class KhuGlobalHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(KhuGlobalHubApplication.class, args);
    }
}
