package com.khu.globalhub.domain.qna.repository;

import com.khu.globalhub.domain.qna.entity.QnA;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnARepository extends JpaRepository<QnA, Long> {

    Page<QnA> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<QnA> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);
}
