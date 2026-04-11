package com.khu.globalhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling        // 멘토-멘티 자동 매칭 스케줄러 활성화
public class KhuGlobalHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(KhuGlobalHubApplication.class, args);
    }
}
