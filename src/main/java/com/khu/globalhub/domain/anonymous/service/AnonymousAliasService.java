package com.khu.globalhub.domain.anonymous.service;

import com.khu.globalhub.domain.anonymous.entity.AnonymousAlias;
import com.khu.globalhub.domain.anonymous.repository.AnonymousAliasRepository;
import com.khu.globalhub.shared.enums.AliasContextType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 익명 번호 할당 서비스.
 * 동일 (contextType, contextId, memberId) 조합이면 항상 같은 번호를 반환한다.
 */
@Service
@RequiredArgsConstructor
public class AnonymousAliasService {

    private final AnonymousAliasRepository repository;

    /**
     * 멤버에게 익명 번호를 할당하거나 기존 번호를 반환한다.
     * 작성 시점에 호출 — 같은 멤버가 같은 컨텍스트에서 재호출해도 동일 번호.
     * @return "익명1", "익명2", ... 형식 문자열
     */
    @Transactional
    public String assign(AliasContextType contextType, Long contextId, Long memberId) {
        return repository
                .findByContextTypeAndContextIdAndMemberId(contextType, contextId, memberId)
                .map(a -> "익명" + a.getAliasNumber())
                .orElseGet(() -> {
                    int next = repository
                            .findMaxAliasNumber(contextType, contextId)
                            .map(max -> max + 1)
                            .orElse(1);
                    repository.save(AnonymousAlias.builder()
                            .contextType(contextType)
                            .contextId(contextId)
                            .memberId(memberId)
                            .aliasNumber(next)
                            .build());
                    return "익명" + next;
                });
    }

    /**
     * 읽기 전용 조회 — 작성 기록이 없으면 "익명" (번호 없음) 반환.
     */
    @Transactional(readOnly = true)
    public String lookup(AliasContextType contextType, Long contextId, Long memberId) {
        return repository
                .findByContextTypeAndContextIdAndMemberId(contextType, contextId, memberId)
                .map(a -> "익명" + a.getAliasNumber())
                .orElse("익명");
    }
}
