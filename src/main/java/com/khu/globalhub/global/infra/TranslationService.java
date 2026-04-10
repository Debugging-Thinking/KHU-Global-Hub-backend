package com.khu.globalhub.global.infra;

import com.khu.globalhub.domain.board.entity.Post;
import com.khu.globalhub.domain.board.entity.PostTranslation;
import com.khu.globalhub.domain.board.repository.PostTranslationRepository;
import com.khu.globalhub.domain.comment.entity.Comment;
import com.khu.globalhub.domain.comment.entity.CommentTranslation;
import com.khu.globalhub.domain.comment.repository.CommentTranslationRepository;
import com.khu.globalhub.domain.qna.entity.Answer;
import com.khu.globalhub.domain.qna.entity.AnswerTranslation;
import com.khu.globalhub.domain.qna.entity.QnA;
import com.khu.globalhub.domain.qna.entity.QnATranslation;
import com.khu.globalhub.domain.qna.repository.AnswerTranslationRepository;
import com.khu.globalhub.domain.qna.repository.QnATranslationRepository;
import com.khu.globalhub.global.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Azure Translator를 이용한 비동기 번역 서비스.
 *
 * 게시글/댓글 저장 직후 @Async로 호출된다.
 * 번역 실패 시 조용히 무시 → 해당 언어의 PostTranslation/CommentTranslation row가 없음
 * → 프론트에서 번역 없음 처리 (번역 버튼 미표시, 원문 표시)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

    private final PostTranslationRepository postTranslationRepository;
    private final CommentTranslationRepository commentTranslationRepository;
    private final QnATranslationRepository qnaTranslationRepository;
    private final AnswerTranslationRepository answerTranslationRepository;

    @Value("${azure.translator.key}")
    private String translatorKey;

    @Value("${azure.translator.endpoint}")
    private String endpoint;

    @Value("${azure.translator.region}")
    private String region;

    /**
     * 게시글 비동기 번역.
     * 원문 언어를 제외한 나머지 5개 언어로 번역 후 저장.
     */
    @Async("translationExecutor")
    public void translatePost(Post post, String title, String content, Language sourceLanguage) {
        List<Language> targets = Arrays.stream(Language.values())
                .filter(l -> l != sourceLanguage)
                .toList();

        try {
            List<Map<String, Object>> results = callAzureTranslator(
                    List.of(title, content), sourceLanguage, targets
            );
            if (results == null) return;

            // results[0] = title 번역, results[1] = content 번역
            List<?> titleTranslations = (List<?>) results.get(0).get("translations");
            List<?> contentTranslations = (List<?>) results.get(1).get("translations");

            for (int i = 0; i < targets.size(); i++) {
                Language lang = targets.get(i);
                String translatedTitle = extractText(titleTranslations, i);
                String translatedContent = extractText(contentTranslations, i);

                if (translatedTitle != null && translatedContent != null) {
                    postTranslationRepository.findByPostIdAndLanguage(post.getId(), lang)
                            .ifPresentOrElse(
                                    t -> t.updateContent(translatedTitle, translatedContent),
                                    () -> postTranslationRepository.save(PostTranslation.builder()
                                            .post(post).language(lang)
                                            .title(translatedTitle).content(translatedContent)
                                            .build())
                            );
                }
            }
        } catch (Exception e) {
            log.warn("Post translation failed [postId={}]: {}", post.getId(), e.getMessage());
        }
    }

    /**
     * 댓글 비동기 번역.
     */
    @Async("translationExecutor")
    public void translateComment(Comment comment, String content, Language sourceLanguage) {
        List<Language> targets = Arrays.stream(Language.values())
                .filter(l -> l != sourceLanguage)
                .toList();

        try {
            List<Map<String, Object>> results = callAzureTranslator(
                    List.of(content), sourceLanguage, targets
            );
            if (results == null) return;

            List<?> contentTranslations = (List<?>) results.get(0).get("translations");

            for (int i = 0; i < targets.size(); i++) {
                Language lang = targets.get(i);
                String translated = extractText(contentTranslations, i);

                if (translated != null) {
                    commentTranslationRepository.findByCommentIdAndLanguage(comment.getId(), lang)
                            .ifPresentOrElse(
                                    t -> t.updateContent(translated),
                                    () -> commentTranslationRepository.save(CommentTranslation.builder()
                                            .comment(comment).language(lang).content(translated)
                                            .build())
                            );
                }
            }
        } catch (Exception e) {
            log.warn("Comment translation failed [commentId={}]: {}", comment.getId(), e.getMessage());
        }
    }

    /** Q&A 질문 비동기 번역. */
    @Async("translationExecutor")
    public void translateQnA(QnA qna, String title, String content, Language sourceLanguage) {
        List<Language> targets = Arrays.stream(Language.values())
                .filter(l -> l != sourceLanguage)
                .toList();

        try {
            List<Map<String, Object>> results = callAzureTranslator(
                    List.of(title, content), sourceLanguage, targets
            );
            if (results == null) return;

            List<?> titleTranslations = (List<?>) results.get(0).get("translations");
            List<?> contentTranslations = (List<?>) results.get(1).get("translations");

            for (int i = 0; i < targets.size(); i++) {
                Language lang = targets.get(i);
                String translatedTitle = extractText(titleTranslations, i);
                String translatedContent = extractText(contentTranslations, i);

                if (translatedTitle != null && translatedContent != null) {
                    qnaTranslationRepository.findByQnaIdAndLanguage(qna.getId(), lang)
                            .ifPresentOrElse(
                                    t -> t.updateContent(translatedTitle, translatedContent),
                                    () -> qnaTranslationRepository.save(QnATranslation.builder()
                                            .qna(qna).language(lang)
                                            .title(translatedTitle).content(translatedContent)
                                            .build())
                            );
                }
            }
        } catch (Exception e) {
            log.warn("QnA translation failed [qnaId={}]: {}", qna.getId(), e.getMessage());
        }
    }

    /** Q&A 답변 비동기 번역. */
    @Async("translationExecutor")
    public void translateAnswer(Answer answer, String content, Language sourceLanguage) {
        List<Language> targets = Arrays.stream(Language.values())
                .filter(l -> l != sourceLanguage)
                .toList();

        try {
            List<Map<String, Object>> results = callAzureTranslator(
                    List.of(content), sourceLanguage, targets
            );
            if (results == null) return;

            List<?> contentTranslations = (List<?>) results.get(0).get("translations");

            for (int i = 0; i < targets.size(); i++) {
                Language lang = targets.get(i);
                String translated = extractText(contentTranslations, i);

                if (translated != null) {
                    answerTranslationRepository.findByAnswerIdAndLanguage(answer.getId(), lang)
                            .ifPresentOrElse(
                                    t -> t.updateContent(translated),
                                    () -> answerTranslationRepository.save(AnswerTranslation.builder()
                                            .answer(answer).language(lang).content(translated)
                                            .build())
                            );
                }
            }
        } catch (Exception e) {
            log.warn("Answer translation failed [answerId={}]: {}", answer.getId(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> callAzureTranslator(
            List<String> texts, Language source, List<Language> targets) {
        String toParams = targets.stream()
                .map(l -> "to=" + l.toAzureCode())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        List<Map<String, String>> body = texts.stream()
                .map(t -> Map.of("Text", t))
                .toList();

        String url = endpoint + "/translate?api-version=3.0&from=" + source.toAzureCode() + "&" + toParams;

        return RestClient.create()
                .post()
                .uri(url)
                .header("Ocp-Apim-Subscription-Key", translatorKey)
                .header("Ocp-Apim-Subscription-Region", region)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(List.class);
    }

    @SuppressWarnings("unchecked")
    private String extractText(List<?> translations, int index) {
        try {
            Map<String, Object> t = (Map<String, Object>) translations.get(index);
            return (String) t.get("text");
        } catch (Exception e) {
            return null;
        }
    }
}
