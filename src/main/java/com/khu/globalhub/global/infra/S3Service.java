package com.khu.globalhub.global.infra;

import com.khu.globalhub.domain.board.entity.Post;
import com.khu.globalhub.domain.board.entity.PostImage;
import com.khu.globalhub.domain.board.repository.PostImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;
import java.util.UUID;

/**
 * AWS S3 비동기 업로드 서비스.
 *
 * 게시글 저장 후 @Async로 호출된다.
 * MultipartFile은 요청 스코프가 끝날 수 있어, byte[]로 미리 읽어 전달한다.
 * 업로드 실패 시 조용히 무시 → 해당 이미지만 없는 게시글로 표시됨.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final PostImageRepository postImageRepository;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * @param imageDataList 각 이미지의 [bytes, contentType] 쌍 목록
     */
    @Async("s3Executor")
    public void uploadPostImages(Post post, List<ImageData> imageDataList) {
        for (int i = 0; i < imageDataList.size(); i++) {
            ImageData imageData = imageDataList.get(i);
            try {
                String key = "posts/" + post.getId() + "/" + UUID.randomUUID();
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(imageData.contentType())
                                .build(),
                        RequestBody.fromBytes(imageData.bytes())
                );
                String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
                postImageRepository.save(PostImage.builder()
                        .post(post).imageUrl(url).orderIndex(i).build());

            } catch (Exception e) {
                log.warn("S3 upload failed [postId={}, index={}]: {}", post.getId(), i, e.getMessage());
            }
        }
    }

    /**
     * 프로필 이미지 S3 업로드 (동기).
     * 키 패턴: profiles/{memberId}/{uuid}
     *
     * @return 업로드된 이미지의 S3 URL
     */
    public String uploadProfileImage(Long memberId, byte[] bytes, String contentType) {
        String key = "profiles/" + memberId + "/" + UUID.randomUUID();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes)
        );
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    public record ImageData(byte[] bytes, String contentType) {}
}
