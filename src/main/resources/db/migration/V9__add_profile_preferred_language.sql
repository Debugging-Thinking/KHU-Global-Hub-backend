-- 선호 언어를 Azure 코드(6개 외 언어 포함, 예 'fr','ja')로 저장하는 컬럼 추가.
-- 기존 language(enum 6개)는 "버킷"으로 유지(6개면 그 언어, 그 외면 EN) → 정적 UI/사전번역 선택에 계속 사용.
-- preferred_language 는 on-demand 번역(콘텐츠/채팅)의 목표 언어로 사용한다.
-- nullable: 레거시/직접삽입 행 깨지지 않게. 응답 조립 시 language 코드로 폴백.

ALTER TABLE profiles ADD COLUMN preferred_language VARCHAR(20);

-- 기존 행 backfill: language enum → Azure 코드 (Language.toAzureCode와 일치)
UPDATE profiles SET preferred_language = CASE language
    WHEN 'KO' THEN 'ko'
    WHEN 'EN' THEN 'en'
    WHEN 'ZH' THEN 'zh-Hans'
    WHEN 'VI' THEN 'vi'
    WHEN 'UZ' THEN 'uz'
    WHEN 'MN' THEN 'mn-Cyrl'
    ELSE 'en'
END
WHERE preferred_language IS NULL;
