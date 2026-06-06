package com.khu.globalhub.translation.application;

import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.infra.AzureTranslateClient;
import com.khu.globalhub.translation.presentation.dto.TranslateRequest;
import com.khu.globalhub.translation.presentation.dto.TranslateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 6개 외 언어 사용자(정적 UI=EN, 콘텐츠=원문)와 채팅의 on-demand 번역 유스케이스.
 * 사전번역(6개 언어)은 {@link com.khu.globalhub.shared.infra.TranslationService}가 담당하고,
 * 여기서는 조회자가 "번역하기"를 눌렀을 때 임의 텍스트를 목표 언어로 즉시 번역한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateService {

    private final AzureTranslateClient azureClient;

    public TranslateResponse translate(TranslateRequest request) {
        try {
            List<AzureTranslateClient.TranslatedText> results =
                    azureClient.translate(request.texts(), List.of(request.target()), request.source());

            if (results.isEmpty()) {
                // 입력이 비었거나 응답 없음 → 원문 그대로 돌려준다.
                return new TranslateResponse(request.texts(), null);
            }

            List<String> translations = results.stream()
                    .map(r -> r.translations().isEmpty() ? null : r.translations().get(0))
                    .toList();
            String detected = results.get(0).detectedLanguage();
            return new TranslateResponse(translations, detected);
        } catch (Exception e) {
            log.warn("On-demand translation failed [target={}]: {}", request.target(), e.getMessage());
            throw new CustomException(ErrorCode.TRANSLATION_FAILED);
        }
    }
}
