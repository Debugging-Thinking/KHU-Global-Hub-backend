package com.khu.globalhub.qna.infrastructure;

import com.khu.globalhub.qna.domain.QnALike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnALikeRepository extends JpaRepository<QnALike, Long> {

    boolean existsByMemberIdAndQnaId(Long memberId, Long qnaId);

    void deleteByMemberIdAndQnaId(Long memberId, Long qnaId);

    void deleteByQnaId(Long qnaId);
}
