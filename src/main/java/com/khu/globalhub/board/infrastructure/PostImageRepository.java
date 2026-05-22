package com.khu.globalhub.board.infrastructure;

import com.khu.globalhub.board.domain.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}
