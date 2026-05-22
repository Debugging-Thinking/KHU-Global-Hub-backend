package com.khu.globalhub.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정.
 * 번역(translationExecutor)과 S3 업로드(s3Executor)를 별도 스레드풀로 분리한다.
 * 게시글/댓글 저장 후 이 두 작업이 백그라운드에서 실행된다.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "translationExecutor")
    public Executor translationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("translation-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "s3Executor")
    public Executor s3Executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("s3-upload-");
        executor.initialize();
        return executor;
    }
}
