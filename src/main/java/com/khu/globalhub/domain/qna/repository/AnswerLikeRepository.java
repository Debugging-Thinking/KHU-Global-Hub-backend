package com.khu.globalhub.domain.qna.repository;

import com.khu.globalhub.domain.qna.entity.AnswerLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerLikeRepository extends JpaRepository<AnswerLike, Long> {

    boolean existsByMemberIdAndAnswerId(Long memberId, Long answerId);

    void deleteByMemberIdAndAnswerId(Long memberId, Long answerId);
}
