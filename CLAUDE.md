# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.1 / Java 17 backend for a KB 청년(youth) housing-finance service: signup/login, a mock "마이데이터"(MyData) financial snapshot sync, KB product listings, and 온통청년(Youth Center) policy recommendations. Package root: `com.moveout.kb_backend` (note the underscore — `com.moveout.kb-backend` is an invalid package name, per `HELP.md`).

## Commands

```bash
./gradlew build                 # compile + test + package
./gradlew bootRun               # run the app locally (needs DB_PASSWORD, JWT_SECRET, youth.policy.api.key env vars)
./gradlew test                  # run all tests
./gradlew test --tests "com.moveout.kb_backend.auth.service.AuthServiceTest"          # single class
./gradlew test --tests "com.moveout.kb_backend.auth.service.AuthServiceTest.signup_성공하면_UUID를_발급한다"  # single method
```

On Windows use `gradlew.bat` instead of `./gradlew`.

Required env vars (see `src/main/resources/application.properties`): `DB_USERNAME` (defaults to `root`), `DB_PASSWORD`, `JWT_SECRET`, `youth.policy.api.key`. MySQL must be reachable at `localhost:3306/kbdb`; `spring.jpa.hibernate.ddl-auto=update` means schema is auto-migrated on boot, there are no manual migration scripts.

## Architecture

Feature-package layout under `src/main/java/com/moveout/kb_backend/`, each with its own `controller/service/dto/entity/repository` subfolders: `auth`, `user`, `mydata`, `policy`, `product`, plus shared `common/` (exceptions, error DTO) and `config/` (Spring Security).

**Auth & security**
- Stateless JWT auth. `JwtAuthenticationFilter` reads `Authorization: Bearer {token}`, validates it via `JwtProvider`, and sets a `UsernamePasswordAuthenticationToken` whose **principal is the raw `UUID` user id** (not a `UserDetails`/`loginId`). Controllers that need the current user do `(UUID) authentication.getPrincipal()` — see `UserController`, `MyDataController`.
- `SecurityConfig` permits `/api/auth/signup`, `/api/auth/login`, `/api/auth/reissue`, `/api/policy/**` without auth; everything else requires a valid access token.
- Access token TTL 24h, refresh token TTL 14 days (`jwt.access-token-expiration` / `jwt.refresh-token-expiration` in `application.properties`). Refresh token is sent/received via the `Refresh-Token` header (not a cookie), and is persisted on the `User` row so `/api/auth/reissue` can check it matches before reissuing an access token.
- Full auth spec (validation rules, entity fields, error-response contract) lives in `src/main/java/com/moveout/kb_backend/auth/authinstructions.md` — read it before touching signup/login/reissue behavior.

**Error handling**
- All business errors throw `common.exception.BusinessException(errorCode, message)`, caught by `GlobalExceptionHandler` and rendered as `common.exception.ErrorResponse` (`{success, errorCode, message, timestamp}`). Error codes are domain-prefixed (`AUTH_001`, `MYDATA_001`, `COMMON_001`, ...).
- There are **two** `ErrorResponse` classes — `common.dto.ErrorResponse` (`{code, message}`) and `common.exception.ErrorResponse` (the one actually wired into `GlobalExceptionHandler`). Use the latter; the former is a stale duplicate, don't add new usages of it.

**MyData sync** (`mydata/`)
- No real external MyData integration. `MyDataService` loads a static mock (`src/main/resources/mock/mydata-mock.json`, deserialized via `MyDataMock`) at startup (`@PostConstruct`) and upserts it onto the user's single `MyDataSnapshot` row on every `/api/mydata/sync` call — one snapshot per user, no history, always overwritten.

**Policy recommendation** (`policy/`)
- `YouthPolicyClient` calls the real 온통청년 (Youth Center) government API over XML (`YouthPolicyXmlResponse`), parsed with `jackson-dataformat-xml`. `/api/policy/sync` pulls and upserts into `YouthPolicyRepository` (dedup by `bizId`); `/api/policy/recommend` reads from that local DB table (falling back to hardcoded mock policies if the table is empty). `YouthPolicyClient.fetchPolicyData` is a second, unused-by-controller path that hits the API live with a different fallback — kept for compatibility, not part of the current request flow.

**Product listing** (`product/`)
- `KbCoreBankingClient` is an interface with a `MockKbCoreBankingClient` implementation, but `ProductController.getProducts()` currently returns its own hardcoded `Map` list and does not call the client at all — the abstraction exists but isn't wired into the controller yet.

## Notes

- Lombok is used throughout (`@RequiredArgsConstructor`, `@Getter`, builders) — an annotation processor, not reflection magic; check generated accessors/constructors match usage when editing entities/DTOs.
- Entities use `UUID` primary keys generated in Java (`UUID.randomUUID()` field default on `User`), not DB-generated identity/sequence columns.
- Korean is used for user-facing strings, error messages, and many test names — match existing language/tone when adding to these.
