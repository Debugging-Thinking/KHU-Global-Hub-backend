package com.khu.globalhub.shared.anonymous.repository;

import com.khu.globalhub.shared.anonymous.entity.AnonymousAlias;
import com.khu.globalhub.shared.enums.AliasContextType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnonymousAliasRepository extends JpaRepository<AnonymousAlias, Long> {

    Optional<AnonymousAlias> findByContextTypeAndContextIdAndMemberId(
            AliasContextType contextType, Long contextId, Long memberId);

    @Query("SELECT MAX(a.aliasNumber) FROM AnonymousAlias a " +
           "WHERE a.contextType = :contextType AND a.contextId = :contextId")
    Optional<Integer> findMaxAliasNumber(
            @Param("contextType") AliasContextType contextType,
            @Param("contextId") Long contextId);
}
