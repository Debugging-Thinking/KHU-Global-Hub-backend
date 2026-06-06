package com.khu.globalhub.coursereview.infrastructure;

import com.khu.globalhub.coursereview.domain.Lecture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    /** 수강편람 재수집 upsert 식별 키. */
    Optional<Lecture> findByCodeAndSemester(String code, String semester);

    @Query("""
            SELECT l FROM Lecture l
            WHERE l.semester = :semester
              AND (:q = '' OR LOWER(l.name) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(l.professor) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(l.code) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY l.name ASC
            """)
    Page<Lecture> search(@Param("semester") String semester, @Param("q") String q, Pageable pageable);
}
