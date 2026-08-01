# kb-backend

KB 청년 주거금융 서비스 백엔드 (회원가입/로그인, 마이데이터 연동, 청년 정책 추천, 자금 시뮬레이션).

## Prerequisites

- Java 17
- 로컬에서 실행 중인 MySQL, `kbdb` 스키마 생성 필요
  ```sql
  CREATE DATABASE kbdb;
  ```
- Gradle은 wrapper(`gradlew`/`gradlew.bat`)로 실행하므로 별도 설치 불필요

## Setup

1. `.env.example`을 복사해 `.env` 생성
   ```
   copy .env.example .env
   ```
   (macOS/Linux: `cp .env.example .env`)
2. `.env`에서 `YOUTH_POLICY_API_KEY`를 온통청년 오픈API 발급키로 채우기 (나머지 값은 로컬 개발용 기본값 그대로 사용 가능)

## Run

```
gradlew.bat bootRun
```
(`./gradlew bootRun` on macOS/Linux)

`bootRun` 실행 시 `.env` 파일을 읽어 환경변수로 주입한다 (`build.gradle` 참고). MySQL이 `localhost:3306`에서 접속 가능해야 한다.

## Test

```
gradlew.bat test
```

## 환경변수

| 변수 | 기본값 | 비고 |
|---|---|---|
| `DB_USERNAME` | `root` | |
| `DB_PASSWORD` | `1234` | 배포 환경에서는 반드시 재정의 |
| `JWT_SECRET` | 로컬 개발용 고정값 | 배포 환경에서는 반드시 재정의 |
| `YOUTH_POLICY_API_KEY` | 없음 (필수) | 없으면 앱이 기동되지 않음 |

## API / 모듈 문서

- [docs/apis.md](docs/apis.md) — 전체 API 목록
- [docs/auth.md](docs/auth.md) — 인증(회원가입/로그인/토큰 재발급) 흐름
- [docs/forecast.md](docs/forecast.md) — 자취 목표 설정 및 자금 시뮬레이션

## 프론트엔드 연동 참고

- CORS 허용 origin: `http://localhost:3000`, `http://localhost:5173`
- 로그인/재발급 응답의 액세스·리프레시 토큰은 JSON 바디가 아니라 응답 헤더(`Authorization`, `Refresh-Token`)로 내려온다
