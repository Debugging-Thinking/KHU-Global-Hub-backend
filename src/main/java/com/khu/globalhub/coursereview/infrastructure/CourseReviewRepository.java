package com.khu.globalhub.coursereview.infrastructure;

import com.khu.globalhub.coursereview.domain.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    List<CourseReview> findByLectureIdOrderByCreatedAtDesc(Long lectureId);

    int countByLectureId(Long lectureId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM CourseReview r WHERE r.lectureId = :lectureId")
    double avgRating(@Param("lectureId") Long lectureId);
}
