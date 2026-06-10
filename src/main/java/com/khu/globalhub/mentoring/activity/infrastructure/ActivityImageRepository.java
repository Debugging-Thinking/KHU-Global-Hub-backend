package com.khu.globalhub.mentoring.activity.infrastructure;

import com.khu.globalhub.mentoring.activity.domain.ActivityImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityImageRepository extends JpaRepository<ActivityImage, Long> {
}