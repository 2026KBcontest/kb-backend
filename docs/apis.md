# API 목록 (auth / user / mydata / forecast)

이 세션에서 작성한 4개 모듈의 전체 API 정리. 공통 사항:

- Base path 없음, 전부 `/api/**`
- 인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더 필요
  (`/api/auth/signup`, `/api/auth/login`, `/api/auth/reissue`, `/api/policy/**` 제외 전부 인증 필요)
- 에러 응답 공통 포맷: `{ "success": false, "errorCode": "...", "message": "...", "timestamp": "..." }`
- 인증 실패(토큰 없음/무효/만료)는 `401`로 응답하며 위와 동일한 포맷을 그대로 사용한다
  (`AUTH_004`/`AUTH_005`, `JwtAuthenticationFilter` + `SecurityConfig`의 `AuthenticationEntryPoint`가 처리 —
  그 외 `BusinessException`은 전부 `GlobalExceptionHandler`가 `400`으로 응답)
- 금액 단위는 전부 원(KRW), 정수(Long)

| Method | Path | 인증 | 모듈 |
|---|---|---|---|
| POST | `/api/auth/signup` | 불필요 | auth |
| POST | `/api/auth/login` | 불필요 | auth |
| POST | `/api/auth/reissue` | 불필요 (Refresh-Token 헤더) | auth |
| GET | `/api/users/me` | 필요 | user |
| PATCH | `/api/users/me/income` | 필요 | user |
| GET | `/api/mydata` | 필요 | mydata |
| POST | `/api/mydata/sync` | 필요 | mydata |
| GET | `/api/forecast` | 필요 | forecast |
| POST | `/api/forecast/simulate` | 필요 | forecast |

---

## auth

### `POST /api/auth/signup`
회원가입.

**Request**
```json
{
  "loginId": "testUser",
  "password": "abcdefg!123",
  "email": "test@example.com",
  "name": "홍길동",
  "birthDate": "1998-01-01",
  "gender": "남성",
  "job": "직장인",
  "residenceRegion": "서울특별시",
  "phone": "010-1234-5678"
}
```
- `loginId`: 영문 대소문자와 숫자 4~20자
- `password`: 10~22자 + 특수문자 1개 이상
- `email`: 표준 이메일 형식
- `birthDate`: `YYYY-MM-DD`, 과거 날짜
- `gender`: `"남성"` | `"여성"`
- `job`: `"학생"` | `"무직"` | `"직장인"`
- `residenceRegion`: 시·도 문자열 (예: `"서울특별시"`) — forecast의 `region`(서울 25개구), policy의 `region`(시·도)과 이름이 겹치지 않도록 의도적으로 `residenceRegion`으로 명명
- `phone`: `010-XXXX-XXXX` 형식만 허용

**Response 200**
```json
{
  "userId": "3a19cd0a-8912-4e83-8cce-8ddfe3426904",
  "message": "회원가입이 완료되었습니다."
}
```

**에러**: `AUTH_001`(loginId 중복), `AUTH_002`(email 중복), `COMMON_001`(형식 검증 실패)

---

### `POST /api/auth/login`
로그인. 토큰은 응답 바디가 아니라 **헤더**로 내려온다.

**Request**
```json
{ "loginId": "testUser", "password": "abcdefg!123" }
```

**Response 200**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Refresh-Token: eyJhbGciOiJIUzI1NiJ9...
```
```json
{
  "userId": "3a19cd0a-8912-4e83-8cce-8ddfe3426904",
  "loginId": "testUser",
  "name": "홍길동"
}
```

**에러**: `AUTH_003`(아이디 없음 또는 비밀번호 불일치 — 동일 코드로 통일)

---

### `POST /api/auth/reissue`
Access token 재발급. 바디 없이 헤더로 refresh token을 보낸다.

**Request**
```
Refresh-Token: eyJhbGciOiJIUzI1NiJ9...
```

**Response 200** (바디 없음)
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...(새 access token)
```

**에러**: `AUTH_004`(서명/만료 무효, 또는 서버에 저장된 refresh token과 불일치)

---

## user

### `GET /api/users/me`
로그인한 사용자 정보 조회 (홈 화면 인사말·소득 표시용, 새로고침해도 값이 유지되도록).

**Response 200**
```json
{
  "loginId": "testUser",
  "name": "홍길동",
  "email": "test@example.com",
  "monthlyIncome": 3000000
}
```

**에러**: `USER_001`(토큰의 userId에 해당하는 사용자가 없는 경우 — 방어적 체크)

---

### `PATCH /api/users/me/income`
로그인한 사용자의 월평균소득을 설정/수정. 응답 바디 없음(200만 반환).

**Request**
```json
{ "monthlyIncome": 3000000 }
```
- `monthlyIncome`: null 불가, 0 이상

**Response**: `200 OK` (바디 없음)

**에러**: `USER_001`(토큰의 userId에 해당하는 사용자가 없는 경우 — 방어적 체크), `COMMON_001`

---

## mydata

### `GET /api/mydata`
가장 최근 마이데이터 스냅샷 조회(연동 실행 없이 읽기만). 응답 형식은 `POST /sync`와 동일.

**에러**: `MYDATA_001`(사용자 없음 — 방어적 체크), `MYDATA_002`(아직 연동 이력이 없음 — `sync`를 먼저 호출해야 함)

---

### `POST /api/mydata/sync`
마이데이터 연동(mock). 매 호출마다 고정 mock 파일(`mock/mydata-mock.json`) 값을 그대로
현재 사용자의 스냅샷에 upsert. 요청 바디 없음.

**Response 200**
```json
{
  "food": 350000,
  "culture": 120000,
  "shopping": 200000,
  "etc": 80000,
  "fixedCostTransport": 90000,
  "fixedCostTelecom": 60000,
  "fixedCostInsurance": 150000,
  "fixedCostSubscription": 30000,
  "fixedCostLoanInterest": 100000,
  "fixedCostHousing": 500000,
  "assetDeposit": 5000000,
  "assetSaving": 3000000,
  "assetInvestment": 10000000,
  "assetLoan": 20000000,
  "assetRemainingRepayment": 15000000
}
```
값은 항상 mock 파일과 동일 (난수 아님, 재호출해도 같은 값).

**에러**: `MYDATA_001`(토큰의 userId에 해당하는 사용자가 없는 경우 — 방어적 체크)

---

## forecast

### `GET /api/forecast`
가장 최근 시뮬레이션 결과 조회(재계산 없이 읽기만). 응답 형식은 `POST /simulate`와 동일.

**에러**: `SIMULATION_004`(아직 저장된 시뮬레이션 결과가 없음 — `simulate`를 먼저 호출해야 함)

---

### `POST /api/forecast/simulate`
자취 목표(지역/계약방식) 기준 필요금액 산출 + 자취 시작 가능 시점 예측. 결과는 사용자당
1건 upsert.

**Request**
```json
{ "region": "강남구", "housingType": "JEONSE" }
```
- `region`: 서울 25개구 이름 (CSV에 존재해야 함)
- `housingType`: `"JEONSE"` | `"WOLSE"`

**Response 200 (정상 예측 가능)**
```json
{
  "region": "강남구",
  "housingType": "JEONSE",
  "deposit": 255270000,
  "monthlyRent": 0,
  "brokerageFee": 765810,
  "requiredAmount": 256035810,
  "currentAsset": 0,
  "monthlySavingCapacity": 600000,
  "isFallbackApplied": true,
  "estimatedMonths": 427,
  "predictedStartDate": "2062-02-28"
}
```

**Response 200 (저축가능금액이 0 이하라 예측 불가한 경우)**
```json
{
  "region": "강북구",
  "housingType": "JEONSE",
  "deposit": 108770000,
  "monthlyRent": 0,
  "brokerageFee": 326310,
  "requiredAmount": 109096310,
  "currentAsset": 0,
  "monthlySavingCapacity": 0,
  "isFallbackApplied": true,
  "estimatedMonths": null,
  "predictedStartDate": null
}
```

**에러**: `SIMULATION_001`(CSV에 없는 지역), `SIMULATION_002`(monthlyIncome 미설정),
`SIMULATION_003`(사용자 없음 — 방어적 체크), `COMMON_001`

---

## 에러 코드 전체

| 코드 | 모듈 | 상황 |
|---|---|---|
| `AUTH_001` | auth | loginId 중복 |
| `AUTH_002` | auth | email 중복 |
| `AUTH_003` | auth | 로그인 실패 |
| `AUTH_004` | auth | 토큰 무효(서명 오류/형식 오류/없음) 또는 refresh token 불일치 — `401` |
| `AUTH_005` | auth | access token 만료 — `401` |
| `USER_001` | user | 사용자 없음 |
| `MYDATA_001` | mydata | 사용자 없음 |
| `MYDATA_002` | mydata | 마이데이터 연동 이력 없음 |
| `SIMULATION_001` | forecast | 지원하지 않는 지역 |
| `SIMULATION_002` | forecast | 소득 정보 없음 |
| `SIMULATION_003` | forecast | 사용자 없음 |
| `SIMULATION_004` | forecast | 저장된 시뮬레이션 결과 없음 |
| `COMMON_001` | 공통 | 요청 값 형식 검증 실패 |
