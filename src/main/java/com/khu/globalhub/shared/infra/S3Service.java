package com.khu.globalhub.shared.infra;

import com.khu.globalhub.board.domain.Post;
import com.khu.globalhub.board.domain.PostImage;
import com.khu.globalhub.board.infrastructure.PostImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                // 원본 파일명을 키 끝에 보존 → 프론트가 첨부 파일명 표시 가능
                String name = safeName(imageData.fileName());
                String key = "posts/" + post.getId() + "/" + UUID.randomUUID()
                        + (name.isBlank() ? "" : "/" + name);
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(imageData.contentType())
                                .build(),
                        RequestBody.fromBytes(imageData.bytes())
                );
                postImageRepository.save(PostImage.builder()
                        .post(post).imageUrl(toUrl(key)).orderIndex(i).build());

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
    /**
     * 범용 파일 S3 업로드 (동기). 키 패턴: uploads/{uuid}/{원본파일명}
     * 댓글/Q&A/답변/채팅 첨부(이미지·일반 파일)가 POST /api/images로 먼저 올린 뒤 URL만 본문에 담는다.
     * 원본 파일명을 키 끝에 보존 → 프론트가 첨부 파일명/아이콘 표시 가능.
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadImage(byte[] bytes, String contentType, String originalName) {
        String name = safeName(originalName);
        String key = "uploads/" + UUID.randomUUID() + (name.isBlank() ? "" : "/" + name);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes)
        );
        return toUrl(key);
    }

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
        return toUrl(key);
    }

    /** 경로 컴포넌트만 남기고 파일명 인코딩 보정. 키는 원문(유니코드 허용), URL에서 인코딩한다. */
    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "";
        return fixEncoding(name.replaceAll(".*[/\\\\]", "").trim());
    }

    /**
     * 멀티파트 파일명 인코딩 보정. Tomcat이 UTF-8 파일명을 ISO-8859-1로 잘못 디코드하면
     * 한글이 깨지는데(모든 문자 ≤0xFF), 이 경우 ISO-8859-1 바이트로 되돌려 UTF-8로 재해석한다.
     * 이미 올바른 유니코드(>0xFF 문자 포함)면 그대로 둔다(ASCII는 변화 없음).
     */
    private static String fixEncoding(String s) {
        boolean looksLatin1 = s.chars().allMatch(c -> c <= 0xFF);
        if (looksLatin1) {
            String reDecoded = new String(s.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            // 재해석 결과에 깨짐문자(U+FFFD)가 없을 때만 채택(원래 ASCII/Latin1이면 그대로 유지)
            if (reDecoded.indexOf('�') < 0) return reDecoded;
        }
        return s;
    }

    /** S3 키 → 퍼블릭 URL. 각 경로 세그먼트를 퍼센트 인코딩(공백/한글 파일명 안전). */
    private String toUrl(String key) {
        StringBuilder sb = new StringBuilder();
        String[] parts = key.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(URLEncoder.encode(parts[i], StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + sb;
    }

    public record ImageData(byte[] bytes, String contentType, String fileName) {}
}
