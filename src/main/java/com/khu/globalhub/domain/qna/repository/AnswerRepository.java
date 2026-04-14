package com.khu.globalhub.domain.qna.repository;

import com.khu.globalhub.domain.qna.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByQnaIdOrderByCreatedAtAsc(Long qnaId);

    /** 답변이 해당 QnA에 속하는지 검증용 */
    boolean existsByIdAndQnaId(Long answerId, Long qnaId);

    /** QnA 목록 페이지에서 답변 수 조회 (N+1 방지) */
    int countByQnaId(Long qnaId);

    /** 한 사람당 1답 제한 검증용 */
    boolean existsByQnaIdAndAuthorId(Long qnaId, Long authorId);
}
