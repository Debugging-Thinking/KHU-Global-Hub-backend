#!/usr/bin/env bash
# KHU Global Hub — 풀스택 로컬 실행 (DB + 백엔드 + 프론트 한 번에) / mac·linux용
#
# 사용법 (backend 폴더에서):
#   bash dev.sh
#
# 프론트 폴더는 backend 와 같은 부모 폴더에 있다고 가정하고 자동 탐지합니다.
# 다른 위치면: GLOBALHUB_FRONTEND=/path/to/frontend bash dev.sh

set -e
BACKEND="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND="${GLOBALHUB_FRONTEND:-}"

# 1) 프론트 폴더 자동 탐지
if [ -z "$FRONTEND" ]; then
  for c in "$BACKEND/../frontend" "$BACKEND/../KHU-Global-Hub-frontend"; do
    if [ -f "$c/package.json" ]; then FRONTEND="$(cd "$c" && pwd)"; break; fi
  done
fi
if [ -z "$FRONTEND" ] || [ ! -f "$FRONTEND/package.json" ]; then
  echo "[X] 프론트 폴더를 찾지 못했습니다. GLOBALHUB_FRONTEND 환경변수로 경로를 지정하세요." >&2
  exit 1
fi

# 2) DB 기동
echo "[1/3] PostgreSQL (docker) 기동..."
docker compose -f "$BACKEND/docker-compose.yml" up -d

# 3a) 백엔드 application-local.yml 보장 (없으면 example 복사 — 더미 DB설정, 비밀키 아님)
if [ ! -f "$BACKEND/src/main/resources/application-local.yml" ]; then
  cp "$BACKEND/src/main/resources/application-local.yml.example" "$BACKEND/src/main/resources/application-local.yml"
  echo "      application-local.yml 생성 (example 복사)"
fi

# 3b) 프론트 .env.local 보장
if [ ! -f "$FRONTEND/.env.local" ]; then
  echo "EXPO_PUBLIC_API_URL=http://localhost:8080" > "$FRONTEND/.env.local"
  echo "      프론트 .env.local 생성 (http://localhost:8080)"
fi

# 4) 백엔드 (백그라운드, 로그 파일)
echo "[2/3] 백엔드 실행 (백그라운드, 로그: /tmp/globalhub-backend.log)..."
( cd "$BACKEND" && ./gradlew bootRun > /tmp/globalhub-backend.log 2>&1 & )

# 5) 프론트 (이 터미널, 포그라운드)
echo "[3/3] 프론트 실행 (이 터미널). 종료: Ctrl+C  ·  DB 종료: docker compose down"
cd "$FRONTEND" && npm run web
