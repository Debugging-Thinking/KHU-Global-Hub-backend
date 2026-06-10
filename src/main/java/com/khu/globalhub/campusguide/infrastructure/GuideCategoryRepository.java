package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.domain.GuideCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideCategoryRepository extends JpaRepository<GuideCategory, Long> {

    /** 조회 시 정렬순서 오름차순으로 노출. */
    List<GuideCategory> findAllByOrderBySortOrderAsc();
}
