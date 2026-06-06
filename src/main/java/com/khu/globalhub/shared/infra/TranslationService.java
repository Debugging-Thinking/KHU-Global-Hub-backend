package com.khu.globalhub.shared.infra;

import com.khu.globalhub.board.domain.Post;
import com.khu.globalhub.board.domain.PostTranslation;
import com.khu.globalhub.board.infrastructure.PostTranslationRepository;
import com.khu.globalhub.board.domain.Comment;
import com.khu.globalhub.board.domain.CommentTranslation;
import com.khu.globalhub.board.infrastructure.CommentTranslationRepository;
import com.khu.globalhub.qna.domain.Answer;
import com.khu.globalhub.qna.domain.AnswerTranslation;
import com.khu.globalhub.qna.domain.QnA;
import com.khu.globalhub.qna.domain.QnATranslation;
import com.khu.globalhub.qna.infrastructure.AnswerTranslationRepository;
import com.khu.globalhub.qna.infrastructure.QnATranslationRepository;
import com.khu.globalhub.shared.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Azure Translator를 이용한 비동기 사전번역 서비스.
 *
 * 게시글/댓글/Q&A/답변 저장 직후 @Async로 호출되어 지원 6개 언어로 번역 행을 저장한다.
 * 실제 HTTP 호출은 {@link AzureTranslateClient}에 위임한다(엔티티 비결합).
 *
 * 원문 언어 자동 감지: 작성자가 고른 언어(claimedLanguage)는 텍스트의 실제 언어와 다를 수 있으므로
 * (예: 한국인이 영어로 작성) from을 지정하지 않아 **원문 언어를 자동 감지**한다.
 * 감지는 translate 응답에 포함되므로 별도 detect 호출이 없어 추가 비용/지연이 없다.
 * 감지된 언어로 원문 행의 라벨을 보정하고, 감지 언어를 제외한 나머지 언어로 번역해 저장한다.
 *
 * 번역 실패 시 조용히 무시 → 해당 언어의 Translation row가 없음 → 프론트에서 원문 표시.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

    private final AzureTranslateClient azureClient;
    private final PostTranslationRepository postTranslationRepository;
    private final CommentTranslationRepository commentTranslationRepository;
    private final QnATranslationRepository qnaTranslationRepository;
    private final AnswerTranslationRepository answerTranslationRepository;

    /** 지원하는 6개 언어 전부 (자동 감지된 원문 언어는 저장 시 제외). */
    private static final List<Language> ALL = Arrays.asList(Language.values());

    /** ALL 언어의 Azure 코드 (translate 호출 대상, 인덱스가 ALL과 정렬됨). */
    private static List<String> targetCodes() {
        return ALL.stream().map(Language::toAzureCode).toList();
    }

    /**
     * 게시글 비동기 번역.
     * 원문 언어를 자동 감지 → 원문 행 라벨 보정 → 감지 언어를 제외한 나머지 언어 번역 저장.
     */
    @Async("translationExecutor")
    public void translatePost(Post post, String title, String content, Language claimedLanguage) {
        try {
            List<AzureTranslateClient.TranslatedText> results =
                    azureClient.translate(List.of(title, content), targetCodes(), null);
            if (results.isEmpty()) return;

            Language source = detectSource(results, claimedLanguage);
            relabelOriginal(claimedLanguage, source, () ->
                    postTranslationRepository.findByPostIdAndLanguage(post.getId(), claimedLanguage)
                            .ifPresent(orig -> { orig.updateLanguage(source); postTranslationRepository.save(orig); }));

            List<String> titleTranslations = results.get(0).translations();
            List<String> contentTranslations = results.get(1).translations();

            for (int i = 0; i < ALL.size(); i++) {
                Language lang = ALL.get(i);
                if (lang == source) continue; // 원문 언어 행은 원문 그대로 유지
                String translatedTitle = at(titleTranslations, i);
                String translatedContent = at(contentTranslations, i);
                if (translatedTitle != null && translatedContent != null) {
                    final Language l = lang;
                    postTranslationRepository.findByPostIdAndLanguage(post.getId(), l)
                            .ifPresentOrElse(
                                    t -> { t.updateContent(translatedTitle, translatedContent); postTranslationRepository.save(t); },
                                    () -> postTranslationRepository.save(PostTranslation.builder()
                                            .post(post).language(l)
                                            .title(translatedTitle).content(translatedContent)
                                            .build())
                            );
                }
            }
        } catch (Exception e) {
            log.warn("Post translation failed [postId={}]: {}", post.getId(), e.getMessage());
        }
    }

    /** 댓글 비동기 번역. */
    @Async("translationExecutor")
    public void translateComment(Comment comment, String content, Language claimedLanguage) {
        try {
            List<AzureTranslateClient.TranslatedText> results =
                    azureClient.translate(List.of(content), targetCodes(), null);
            if (results.isEmpty()) return;

            Language source = detectSource(results, claimedLanguage);
            relabelOriginal(claimedLanguage, source, () ->
                    commentTranslationRepository.findByCommentIdAndLanguage(comment.getId(), claimedLanguage)
                            .ifPresent(orig -> { orig.updateLanguage(source); commentTranslationRepository.save(orig); }));

            List<String> contentTranslations = results.get(0).translations();

            for (int i = 0; i < ALL.size(); i++) {
                Language lang = ALL.get(i);
                if (lang == source) continue;
                String translated = at(contentTranslations, i);
                if (translated != null) {
                    final Language l = lang;
                    commentTranslationRepository.findByCommentIdAndLanguage(comment.getId(), l)
                            .ifPresentOrElse(
                                    t -> { t.updateContent(translated); commentTranslationRepository.save(t); },
                                    () -> commentTranslationRepository.save(CommentTranslation.builder()
                                            .comment(comment).language(l).content(translated)
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
    public void translateQnA(QnA qna, String title, String content, Language claimedLanguage) {
        try {
            List<AzureTranslateClient.TranslatedText> results =
                    azureClient.translate(List.of(title, content), targetCodes(), null);
            if (results.isEmpty()) return;

            Language source = detectSource(results, claimedLanguage);
            relabelOriginal(claimedLanguage, source, () ->
                    qnaTranslationRepository.findByQnaIdAndLanguage(qna.getId(), claimedLanguage)
                            .ifPresent(orig -> { orig.updateLanguage(source); qnaTranslationRepository.save(orig); }));

            List<String> titleTranslations = results.get(0).translations();
            List<String> contentTranslations = results.get(1).translations();

            for (int i = 0; i < ALL.size(); i++) {
                Language lang = ALL.get(i);
                if (lang == source) continue;
                String translatedTitle = at(titleTranslations, i);
                String translatedContent = at(contentTranslations, i);
                if (translatedTitle != null && translatedContent != null) {
                    final Language l = lang;
                    qnaTranslationRepository.findByQnaIdAndLanguage(qna.getId(), l)
                            .ifPresentOrElse(
                                    t -> { t.updateContent(translatedTitle, translatedContent); qnaTranslationRepository.save(t); },
                                    () -> qnaTranslationRepository.save(QnATranslation.builder()
                                            .qna(qna).language(l)
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
    public void translateAnswer(Answer answer, String content, Language claimedLanguage) {
        try {
            List<AzureTranslateClient.TranslatedText> results =
                    azureClient.translate(List.of(content), targetCodes(), null);
            if (results.isEmpty()) return;

            Language source = detectSource(results, claimedLanguage);
            relabelOriginal(claimedLanguage, source, () ->
                    answerTranslationRepository.findByAnswerIdAndLanguage(answer.getId(), claimedLanguage)
                            .ifPresent(orig -> { orig.updateLanguage(source); answerTranslationRepository.save(orig); }));

            List<String> contentTranslations = results.get(0).translations();

            for (int i = 0; i < ALL.size(); i++) {
                Language lang = ALL.get(i);
                if (lang == source) continue;
                String translated = at(contentTranslations, i);
                if (translated != null) {
                    final Language l = lang;
                    answerTranslationRepository.findByAnswerIdAndLanguage(answer.getId(), l)
                            .ifPresentOrElse(
                                    t -> { t.updateContent(translated); answerTranslationRepository.save(t); },
                                    () -> answerTranslationRepository.save(AnswerTranslation.builder()
                                            .answer(answer).language(l).content(translated)
                                            .build())
                            );
                }
            }
        } catch (Exception e) {
            log.warn("Answer translation failed [answerId={}]: {}", answer.getId(), e.getMessage());
        }
    }

    /** 감지된 원문 언어가 작성 시 claimed 언어와 다르면 원문 행 라벨을 보정한다. */
    private void relabelOriginal(Language claimed, Language detected, Runnable relabel) {
        if (detected != claimed) {
            relabel.run();
        }
    }

    /** translate 응답의 detectedLanguage를 앱 enum으로 매핑. 없거나 미지원 언어면 fallback. */
    private Language detectSource(List<AzureTranslateClient.TranslatedText> results, Language fallback) {
        if (results.isEmpty()) return fallback;
        String detected = results.get(0).detectedLanguage();
        return Language.fromAzureCode(detected).orElse(fallback);
    }

    /** 인덱스 범위를 벗어나면 null. */
    private String at(List<String> list, int index) {
        return (list != null && index >= 0 && index < list.size()) ? list.get(index) : null;
    }
}
