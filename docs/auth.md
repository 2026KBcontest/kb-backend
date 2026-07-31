# auth 모듈

회원가입, 로그인, JWT 발급·재발급을 담당하는 모듈. 관련 지시사항 원문은
`src/main/java/com/moveout/kb_backend/auth/authinstructions.md` 참고.

`auth` 패키지 자체에는 엔티티가 없다. 실제 `User` 엔티티/Repository는 `user` 패키지에 있고,
`auth`는 그것을 그대로 가져다 쓰는 "인증 흐름" 전용 모듈이다 (컨트롤러/서비스/JWT 유틸만 포함).

## 전체 흐름

```
회원가입   POST /api/auth/signup  { loginId, password, email, name }
              │
              ▼
        AuthService.signup()
              ├─ 형식 검증 (Bean Validation: @Pattern/@Size/@Email, 컨트롤러 @Valid 단계에서 선행)
              ├─ loginId 중복 확인 (AUTH_001)
              ├─ email 중복 확인 (AUTH_002)
              ├─ 비밀번호 BCrypt 해시 후 User 저장 (id는 UUID.randomUUID()로 즉시 발급)
              ▼
        SignupResponse { userId, message }


로그인    POST /api/auth/login  { loginId, password }
              │
              ▼
        AuthService.login()
              ├─ loginId로 User 조회, 없으면 AUTH_003
              ├─ 비밀번호 matches 확인, 틀리면 AUTH_003 (동일 코드로 통일 - 아이디/비번 중 무엇이
              │   틀렸는지 노출하지 않음)
              ├─ accessToken(24h)/refreshToken(14일) 발급, refreshToken은 User 테이블에 저장
              ▼
        응답 헤더: Authorization: Bearer {accessToken}, Refresh-Token: {refreshToken}
        응답 바디: LoginResponse { userId, loginId, name }  (토큰은 바디에 넣지 않음)


재발급    POST /api/auth/reissue  (헤더: Refresh-Token: {refreshToken})
              │
              ▼
        AuthService.reissue()
              ├─ 토큰 서명·만료 검증 (AUTH_004)
              ├─ 토큰의 userId로 User 조회 (AUTH_004)
              ├─ 전달받은 토큰이 User.refreshToken과 일치하는지 확인 (AUTH_004)
              ▼
        새 accessToken만 발급 (refreshToken은 rotation 없이 그대로 유지, 자기 만료까지 재사용)
        응답 헤더: Authorization: Bearer {newAccessToken}
```

이후 요청은 `Authorization: Bearer {accessToken}` 헤더로 인증한다.
`JwtAuthenticationFilter`가 매 요청마다 토큰을 검증해 `SecurityContext`에 `userId(UUID)`를
principal로 심어두고, 각 컨트롤러는 `Authentication.getPrincipal()`로 그 값을 꺼내 쓴다
(`mydata`/`user`/`forecast` 모듈이 전부 이 방식).

## API

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/signup` | 불필요 | 회원가입 |
| POST | `/api/auth/login` | 불필요 | 로그인, 토큰 발급 |
| POST | `/api/auth/reissue` | 불필요(Refresh-Token 헤더로 대체) | access token 재발급 |

`SecurityConfig`에서 이 세 경로와 `/api/policy/**`만 `permitAll`이고, 나머지 전부
`anyRequest().authenticated()`.

### 필드 검증 규칙 (`SignupRequest`)
| 필드 | 규칙 |
|---|---|
| `loginId` | `^[a-zA-Z0-9]{4,20}$` — 영문 대소문자와 숫자, 4~20자 (특수문자/공백 불가) |
| `password` | 10~22자 + 특수문자 최소 1개 포함 (`@Size` + 정규식 `.*[^a-zA-Z0-9].*`) |
| `email` | `@Email` 표준 이메일 형식 |
| `name` | `@NotBlank` |

**에러 코드** (형식: `{success:false, errorCode, message, timestamp}`, `GlobalExceptionHandler`가
`BusinessException`을 이 형식으로 변환)

| 코드 | 상황 |
|---|---|
| `AUTH_001` | loginId 중복 |
| `AUTH_002` | email 중복 |
| `AUTH_003` | 로그인 실패 (아이디 없음 또는 비밀번호 불일치, 동일 코드) |
| `AUTH_004` | reissue 시 refresh token이 무효(서명/만료/불일치) |
| `COMMON_001` | Bean Validation 실패 (형식 검증, `MethodArgumentNotValidException`) |

## JWT 정책

- 라이브러리: `io.jsonwebtoken`(jjwt), 알고리즘 HS256, 시크릿은 `jwt.secret` 프로퍼티
  (`${JWT_SECRET}` 환경변수, 평문으로 커밋하지 않음)
- Access token 만료: 24시간(`jwt.access-token-expiration=86400000`)
- Refresh token 만료: 14일(`jwt.refresh-token-expiration=1209600000`)
- 토큰 payload는 `subject = userId(UUID 문자열)`만 담음 (별도 claim 없음)
- Refresh token은 **DB(User.refreshToken 컬럼)에 저장**하고 reissue 시 대조 (Redis 미사용 —
  탈취 시 무효화하려면 해당 User 행의 refreshToken을 갱신/삭제하면 됨)
- 두 토큰 모두 응답 바디가 아닌 **응답 헤더**로 전달 (`Authorization`, `Refresh-Token`)

## DB 연관관계

```
User (user 테이블)
  id UUID (PK, 코드에서 UUID.randomUUID()로 직접 발급 — Hibernate 시퀀스/제너레이터 미사용)
  loginId  UNIQUE, NOT NULL
  password NOT NULL (BCrypt 해시)
  email    UNIQUE, NOT NULL
  name     NOT NULL
  monthlyIncome  nullable  (회원가입 시 입력 안 함 — PATCH /api/users/me/income 으로 별도 설정)
  refreshToken   nullable  (로그인 시 갱신)
  createdAt / updatedAt   (BaseTimeEntity, @CreatedDate/@LastModifiedDate 자동 관리)
```

`auth` 모듈은 이 `User` 테이블 하나만 다룬다. `MyDataSnapshot`(mydata 모듈)과
`SimulationResult`(forecast 모듈)는 각각 `User`와 1:1로 연결되지만 `auth` 코드에서 직접 참조하지
않는다.

## 다른 모듈과의 관계

- **user**: `User` 엔티티/`UserRepository`를 그대로 가져다 씀. 회원가입 시 `monthlyIncome`은
  넣지 않고, 이후 `user` 모듈의 `PATCH /api/users/me/income`으로 별도 입력.
- **common.exception**: `BusinessException` + `GlobalExceptionHandler`가 auth뿐 아니라
  전체 프로젝트 공통 에러 포맷을 담당 — `mydata`/`forecast` 모듈도 동일 포맷을 그대로 재사용.
- **config.SecurityConfig**: `PasswordEncoder`(BCrypt) 빈과 `JwtAuthenticationFilter` 등록이
  여기 있음. auth가 발급한 토큰을 이 필터가 매 요청마다 검증.
- **KbBackendApplication**: `@EnableJpaAuditing`이 여기 붙어 있어 `User`의
  `createdAt`/`updatedAt`이 자동 관리됨 (mydata/forecast 엔티티도 동일 메커니즘 사용).

## 데이터베이스

로컬 개발은 MySQL(`localhost:3306/kbdb`)을 사용하며, 접속 계정/비밀번호는
`DB_USERNAME`/`DB_PASSWORD` 환경변수로 주입한다 (`application.properties`에 평문 없음).
`spring.jpa.hibernate.ddl-auto=update`라 엔티티 변경 시 스키마가 자동 갱신된다(운영 환경에서는
재검토 필요).
