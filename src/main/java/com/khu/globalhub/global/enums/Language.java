package com.khu.globalhub.global.enums;

/**
 * 앱에서 지원하는 6개 언어.
 * 유저가 가입 시 선택하며, 게시글/댓글 번역 버전을 저장할 때도 이 값을 사용한다.
 */
public enum Language {
    KO("ko"),
    EN("en"),
    ZH("zh-Hans"),  // Azure Translator는 zh-Hans (간체) 사용
    VI("vi"),
    ES("es"),
    MN("mn");

    private final String azureCode;

    Language(String azureCode) {
        this.azureCode = azureCode;
    }

    /** Azure Translator API에 전달하는 언어 코드 */
    public String toAzureCode() {
        return azureCode;
    }
}
