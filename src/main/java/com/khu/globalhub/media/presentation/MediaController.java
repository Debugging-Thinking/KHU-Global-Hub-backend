package com.khu.globalhub.media.presentation;

import com.khu.globalhub.media.presentation.dto.ImageUploadResponse;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.infra.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 범용 이미지 업로드 (JWT 보호). 댓글/Q&A/답변/채팅이 이미지를 먼저 올리고 URL만 본문에 담는다.
 * 게시글은 작성 multipart에 이미지를 직접 포함(기존 방식)하므로 여기 미사용.
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class MediaController {

    private final S3Service s3Service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> upload(
            @RequestPart("image") MultipartFile image
    ) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        try {
            String url = s3Service.uploadImage(image.getBytes(), image.getContentType(), image.getOriginalFilename());
            return ResponseEntity.ok(ApiResponse.ok(new ImageUploadResponse(url)));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
