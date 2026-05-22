# KHU Global Hub — 풀스택 로컬 실행 (DB + 백엔드 + 프론트 한 번에)
#
# 사용법 (PowerShell, backend 폴더에서):
#   powershell -ExecutionPolicy Bypass -File dev.ps1
#
# 프론트 폴더는 backend 와 같은 부모 폴더에 있다고 가정하고 자동 탐지합니다.
# 다른 위치면: $env:GLOBALHUB_FRONTEND="C:\path\to\frontend" 후 실행, 또는 -FrontendPath 인자 사용.

param([string]$FrontendPath = $env:GLOBALHUB_FRONTEND)

$ErrorActionPreference = "Stop"
$backend = $PSScriptRoot

# 1) 프론트 폴더 자동 탐지
if (-not $FrontendPath) {
    foreach ($c in @("$backend\..\frontend", "$backend\..\KHU-Global-Hub-frontend")) {
        if (Test-Path "$c\package.json") { $FrontendPath = (Resolve-Path $c).Path; break }
    }
}
if (-not $FrontendPath -or -not (Test-Path "$FrontendPath\package.json")) {
    Write-Host "[X] 프론트 폴더를 찾지 못했습니다." -ForegroundColor Red
    Write-Host "    backend 와 frontend 를 같은 부모 폴더에 두거나," -ForegroundColor Red
    Write-Host "    `$env:GLOBALHUB_FRONTEND 로 경로를 지정한 뒤 다시 실행하세요." -ForegroundColor Red
    exit 1
}

# 2) DB 기동
Write-Host "[1/3] PostgreSQL (docker) 기동..." -ForegroundColor Cyan
docker compose -f "$backend\docker-compose.yml" up -d
if ($LASTEXITCODE -ne 0) { Write-Host "[X] docker compose 실패. Docker Desktop 실행 여부 확인." -ForegroundColor Red; exit 1 }

# 3a) 백엔드 application-local.yml 보장 (없으면 example 복사 — 더미 DB설정, 비밀키 아님)
$appLocal = Join-Path $backend "src\main\resources\application-local.yml"
$appExample = Join-Path $backend "src\main\resources\application-local.yml.example"
if (-not (Test-Path $appLocal)) {
    Copy-Item $appExample $appLocal
    Write-Host "      application-local.yml 생성 (example 복사)" -ForegroundColor DarkGray
}

# 3b) 프론트 .env.local 보장 (로컬 백엔드 주소)
$envLocal = Join-Path $FrontendPath ".env.local"
if (-not (Test-Path $envLocal)) {
    "EXPO_PUBLIC_API_URL=http://localhost:8080" | Out-File -FilePath $envLocal -Encoding utf8
    Write-Host "      프론트 .env.local 생성 (http://localhost:8080)" -ForegroundColor DarkGray
}

# 4) 백엔드 새 창
Write-Host "[2/3] 백엔드 실행 (새 창)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$backend'; ./gradlew bootRun"

# 5) 프론트 새 창
Write-Host "[3/3] 프론트 실행 (새 창)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$FrontendPath'; npm run web"

Write-Host ""
Write-Host "완료! 백엔드: http://localhost:8080  ·  프론트: 곧 브라우저에서 열립니다." -ForegroundColor Green
Write-Host "(각 새 창에서 Ctrl+C 로 종료. DB 종료는: docker compose down)" -ForegroundColor DarkGray
