package com.khu.globalhub.domain.qna.repository;

import com.khu.globalhub.domain.qna.entity.QnALike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnALikeRepository extends JpaRepository<QnALike, Long> {

    boolean existsByMemberIdAndQnaId(Long memberId, Long qnaId);

    void deleteByMemberIdAndQnaId(Long memberId, Long qnaId);
}
