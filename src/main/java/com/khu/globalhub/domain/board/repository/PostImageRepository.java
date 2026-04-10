package com.khu.globalhub.domain.board.repository;

import com.khu.globalhub.domain.board.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}
