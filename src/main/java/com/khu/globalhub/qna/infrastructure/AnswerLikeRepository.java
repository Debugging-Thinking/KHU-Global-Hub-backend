package com.khu.globalhub.qna.infrastructure;

import com.khu.globalhub.qna.domain.AnswerLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerLikeRepository extends JpaRepository<AnswerLike, Long> {

    boolean existsByMemberIdAndAnswerId(Long memberId, Long answerId);

    void deleteByMemberIdAndAnswerId(Long memberId, Long answerId);

    void deleteByAnswerId(Long answerId);

    void deleteByAnswerIdIn(java.util.Collection<Long> answerIds);
}
