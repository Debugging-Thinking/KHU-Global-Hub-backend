package com.khu.globalhub.board.infrastructure;

import com.khu.globalhub.board.domain.Post;
import com.khu.globalhub.shared.enums.BoardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** 게시판 종류별 최신순 페이징 */
    Page<Post> findByBoardTypeOrderByCreatedAtDesc(BoardType boardType, Pageable pageable);

    /** 특정 멤버가 작성한 게시글 (프로필 조회용) */
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    /** 특정 멤버가 작성한 비익명 게시글 (타인 프로필 조회용) */
    Page<Post> findByAuthorIdAndIsAnonymousFalseOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    /** 인기 게시물: 좋아요 수 기준 이상인 게시글을 좋아요 많은 순으로 페이징 */
    Page<Post> findByLikeCountGreaterThanEqualOrderByLikeCountDesc(int likeCount, Pageable pageable);
}
