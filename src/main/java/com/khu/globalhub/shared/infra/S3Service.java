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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Async("s3Executor")
    public void uploadPostImages(Post post, List<ImageData> imageDataList) {
        for (int i = 0; i < imageDataList.size(); i++) {
            ImageData imageData = imageDataList.get(i);
            try {
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

    /** 범용 S3 업로드 - 업로드된 URL 목록 반환. folder 예: "activities/1/2" */
    public List<String> uploadImages(String folder, List<ImageData> imageDataList) {
        List<String> urls = new ArrayList<>();
        for (ImageData imageData : imageDataList) {
            try {
                String key = folder + "/" + UUID.randomUUID();
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .contentType(imageData.contentType())
                                .build(),
                        RequestBody.fromBytes(imageData.bytes())
                );
                urls.add(toUrl(key));
            } catch (Exception e) {
                log.warn("S3 upload failed [folder={}]: {}", folder, e.getMessage());
            }
        }
        return urls;
    }

    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "";
        return fixEncoding(name.replaceAll(".*[/\\\\]", "").trim());
    }

    private static String fixEncoding(String s) {
        boolean looksLatin1 = s.chars().allMatch(c -> c <= 0xFF);
        if (looksLatin1) {
            String reDecoded = new String(s.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            if (reDecoded.indexOf('\uFFFD') < 0) return reDecoded;
        }
        return s;
    }

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