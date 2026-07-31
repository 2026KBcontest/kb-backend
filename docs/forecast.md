# forecast 모듈

자취 목표(지역·계약방식)를 입력받아 필요 자금을 산출하고, 사용자의 현재 자산·소득을 근거로
"언제쯤 자취를 시작할 수 있는지"를 예측하는 모듈. 관련 지시사항 원문은
`src/main/java/com/moveout/kb_backend/forecast/forecastinstructions.md` 참고.

## 전체 흐름

```
클라이언트 (region, housingType)
        │  POST /api/forecast/simulate  (Authorization: Bearer <accessToken>)
        ▼
ForecastController
        │  Authentication.getPrincipal() → userId(UUID)  [auth 모듈의 JwtAuthenticationFilter가 세팅]
        ▼
ForecastService.simulate(userId, request)
        │
        ├─ 1) User 조회 (없으면 SIMULATION_003)
        ├─ 2) User.monthlyIncome null 체크 (없으면 SIMULATION_002)
        ├─ 3) RegionHousingFeeLoader에서 region 조회 (CSV 기반, 없으면 SIMULATION_001)
        ├─ 4) 기능1: 보증금/월세/중개수수료/필요금액 계산
        ├─ 5) MyDataSnapshot 조회 (User와 1:1)
        │      ├─ 있으면 → 현재보유금액·월소비총합 계산
        │      └─ 없으면 → 현재보유금액 0, 저축가능금액은 fallback
        ├─ 6) 기능2: 현재보유금액/월저축가능금액/소요개월/예측시작일 계산
        └─ 7) SimulationResult upsert (userId로 존재하면 UPDATE, 없으면 INSERT)
        ▼
SimulationResultResponse (JSON) 반환
```

한 번의 API 호출로 기능1(목표 설정)과 기능2(예측)이 함께 계산되고, 결과는 사용자당 1행으로만
저장된다 (이력 없음, 재호출 시 덮어씀).

## API

### `POST /api/forecast/simulate`

인증 필요 (JWT access token, `Authorization: Bearer {token}`).

**Request**
```json
{ "region": "강남구", "housingType": "JEONSE" }
```
- `region`: 서울 25개구 이름 문자열 (CSV `seoul_housingfee_pergu.csv`에 있는 값과 정확히 일치해야 함)
- `housingType`: `"JEONSE"` | `"WOLSE"`

**Response (200)**
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
`estimatedMonths`/`predictedStartDate`는 저축가능금액이 0 이하라 계산이 불가능한 경우 `null`로
내려간다 (예측 불가 상태를 의미, 에러가 아니라 200 정상 응답).

**에러 코드** (형식은 auth 모듈 공통 포맷 그대로: `{success:false, errorCode, message, timestamp}`)

| 코드 | 상황 |
|---|---|
| `SIMULATION_001` | CSV에 없는 `region` 요청 |
| `SIMULATION_002` | `User.monthlyIncome`이 아직 설정되지 않음 (null) |
| `SIMULATION_003` | 인증 토큰의 userId에 해당하는 User가 존재하지 않음 (방어적 체크) |

## 계산 로직

### 기능 1 — 자취 목표 설정
- **보증금/월세**
  - JEONSE: 보증금 = CSV 해당 지역 평균보증금, 월세 = 0
  - WOLSE: 보증금 = 1,000만원(고정), 월세 = CSV 해당 지역 평균월세
  - CSV(`seoul_housingfee_pergu.csv`)는 단위가 "만원"이라 앱 로딩 시 ×10,000 해서 원 단위로 캐싱
- **거래금액**: `보증금 + 월세×100`, 단 그 값이 5,000만원 미만이면 `보증금 + 월세×70`으로 재계산
- **중개수수료**: 거래금액 구간별 상한요율 × 거래금액, 한도액과 비교해 작은 값
  | 거래금액 | 상한요율 | 한도액 |
  |---|---|---|
  | 5,000만원 미만 | 0.5% | 20만원 |
  | 5,000만원~1억원 미만 | 0.4% | 30만원 |
  | 1억원 이상 | 0.3% | 없음 |

  서울 25개구 전세보증금이 전부 1억원 이상이라, JEONSE는 사실상 항상 마지막 구간(0.3%/무한도)이
  적용된다.
- **필요금액** = 보증금 + (월세 × 2) + 중개수수료

### 기능 2 — 자취 시작 가능 시점 예측
- **현재 보유금액** = (예금+적금+투자자산) − 남은상환금액, MyDataSnapshot이 없으면 0
- **월 저축가능금액** = 월평균소득 − 월소비총합(식비+문화·취미+쇼핑+기타소비+고정비용 6종 합)
  - 계산 결과가 음수이거나 MyDataSnapshot 자체가 없으면 `월평균소득 × 0.2`로 대체(fallback)하고
    `isFallbackApplied = true`
- **소요개월/예측시작일**
  - 현재보유금액 ≥ 필요금액 → 0개월, 오늘 날짜
  - 월저축가능금액(대체값 포함) ≤ 0 → 계산 불가, `null`/`null`
  - 그 외 → `ceil((필요금액-현재보유금액) / 월저축가능금액)` 개월, 오늘 + 그 개월수

## DB 연관관계

```
User (user 테이블)                MyDataSnapshot (my_data_snapshot 테이블)
  id UUID (PK)  ◄───────────1:1───────────  user_id (FK, UNIQUE)
  monthlyIncome                              assetDeposit / assetSaving / assetInvestment
  ...                                        assetLoan / assetRemainingRepayment
      ▲                                      food / culture / shopping / etc / fixedCost*
      │
      │ 1:1 (공유 PK)
      │
SimulationResult (simulation_result 테이블)
  user_id UUID (PK, FK → user.id)   ← 자체 PK 없이 User의 id를 그대로 공유 (@MapsId)
  region, housingType
  deposit, monthlyRent, brokerageFee, requiredAmount
  currentAsset, monthlySavingCapacity, isFallbackApplied
  estimatedMonths, predictedStartDate
  updatedAt
```

- `SimulationResult`는 `User`와 1:1이며 **자체 UUID를 새로 발급하지 않고 User의 PK를 그대로
  공유**한다 (`@OneToOne` + `@MapsId`). `MyDataSnapshot`은 반대로 자체 UUID PK + `user_id`
  UNIQUE FK 방식이라 두 엔티티의 PK 설계가 서로 다르다 — 이는 forecast 지시사항이 스키마에서
  `userId`를 명시적으로 PK로 지정했기 때문.
- forecast 모듈은 **소득/자산 값을 스냅샷으로 복사 저장하지 않는다.** `SimulationResult`에는
  계산 결과만 저장되고, 매 계산마다 `User`/`MyDataSnapshot`을 최신 상태로 다시 조회한다. 즉
  마이데이터를 재연동하면 다음 `/simulate` 호출부터 곧바로 반영되지만, 과거에 계산해둔
  `SimulationResult` 값 자체가 자동으로 갱신되지는 않는다(재호출해야 갱신).
- 이력 테이블 없음 — 재계산 시 기존 행을 UPDATE.

## 다른 모듈과의 관계

forecast는 다음을 **읽기 전용으로 재사용**하며, 어떤 모듈의 코드도 수정하지 않았다.

- **auth**: `JwtAuthenticationFilter`가 세팅한 `Authentication.getPrincipal()`(userId)로 사용자
  식별. 공통 에러 응답 포맷(`BusinessException` + `GlobalExceptionHandler`)도 그대로 재사용.
- **user**: `User.monthlyIncome`(소득), `UserRepository.findById`.
- **mydata**: `MyDataSnapshot`의 자산/소비 필드, `MyDataSnapshotRepository.findByUser`.
- **config/SecurityConfig**: 별도 수정 없음. 기존에 `/api/forecast/**`를 permitAll 목록에 넣지
  않았기 때문에 `anyRequest().authenticated()` 규칙이 자동으로 적용되어 인증이 필요하다.

## 데이터 소스

`src/main/resources/seoul_housingfee_pergu.csv` — 서울 25개구의 평균 전세보증금/평균 월세
(만원 단위). 앱 기동 시 `RegionHousingFeeLoader`가 1회 파싱해 메모리에 캐싱하고,
`/api/forecast/simulate` 호출마다 이 캐시에서 지역을 조회한다(요청마다 파일을 다시 읽지 않음).
