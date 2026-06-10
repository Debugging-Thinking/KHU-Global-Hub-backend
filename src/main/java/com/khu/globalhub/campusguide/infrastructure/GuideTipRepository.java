package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.domain.GuideTip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GuideTipRepository extends JpaRepository<GuideTip, Long> {

    /** 한 카테고리의 팁들 (정렬순서 오름차순). 삭제 정리에도 사용. */
    List<GuideTip> findByCategoryId(Long categoryId);

    /** 가이드 트리 조립 시 여러 카테고리의 팁을 한 번에 (N+1 방지, 정렬순서 오름차순). */
    List<GuideTip> findByCategoryIdInOrderBySortOrderAsc(Collection<Long> categoryIds);

    /** 카테고리 삭제 시 하위 팁 일괄 제거. */
    void deleteByCategoryId(Long categoryId);
}
