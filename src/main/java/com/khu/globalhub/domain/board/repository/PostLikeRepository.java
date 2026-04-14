package com.khu.globalhub.domain.board.repository;

import com.khu.globalhub.domain.board.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    void deleteByMemberIdAndPostId(Long memberId, Long postId);

    void deleteByPostId(Long postId);
}
