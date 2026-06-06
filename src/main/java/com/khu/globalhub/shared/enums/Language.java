package com.khu.globalhub.shared.enums;

import java.util.Optional;

/**
 * 앱에서 지원하는 6개 언어 (한/영/중/베트남/우즈벡/몽골).
 * 유저가 가입 시 선택하며, 게시글/댓글 번역 버전을 저장할 때도 이 값을 사용한다.
 * 괄호 안은 Azure Translator API 언어 코드 (enum 이름과 다를 수 있음).
 */
public enum Language {
    KO("ko"),
    EN("en"),
    ZH("zh-Hans"),    // Azure Translator는 zh-Hans (중국어 간체) 사용
    VI("vi"),
    UZ("uz"),         // 우즈벡어 (라틴)
    MN("mn-Cyrl");    // 몽골어 (키릴) — Azure는 'mn'이 아니라 'mn-Cyrl' 사용 (translate 대상 코드)

    private final String azureCode;

    Language(String azureCode) {
        this.azureCode = azureCode;
    }

    /** Azure Translator API에 전달하는 언어 코드 */
    public String toAzureCode() {
        return azureCode;
    }

    /**
     * Azure가 감지/반환한 언어 코드를 앱 enum으로 역매핑.
     * 감지 코드는 번역 코드와 다를 수 있어(zh/zh-Hant, mn/mn-Mong 등) 접두사로도 매칭한다.
     * 지원 6개 언어가 아니면 empty (예: 불어 → 호출부에서 폴백 처리).
     */
    public static Optional<Language> fromAzureCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        String c = code.toLowerCase();
        if (c.startsWith("zh")) return Optional.of(ZH);
        if (c.startsWith("mn")) return Optional.of(MN);
        for (Language l : values()) {
            if (l.azureCode.equalsIgnoreCase(code)) return Optional.of(l);
        }
        return Optional.empty();
    }
}
