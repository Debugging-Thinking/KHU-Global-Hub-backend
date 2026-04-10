package com.khu.globalhub.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 생성일/수정일을 공통으로 관리하는 추상 클래스.
 * 모든 주요 엔티티가 이 클래스를 상속받아 createdAt, updatedAt을 자동으로 기록한다.
 *
 * @MappedSuperclass : 이 클래스 자체는 테이블이 생성되지 않고,
 *                     상속받은 엔티티의 테이블에 컬럼으로 포함된다.
 * @EnableJpaAuditing : Application 클래스에 선언되어야 동작한다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    // 최초 저장 시 자동 기록, 이후 변경 불가
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 엔티티가 수정될 때마다 자동 갱신
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
