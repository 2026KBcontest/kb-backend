Instruction: Auth 모듈 구현 지침
1. 기술 스택 제약
   Database: MySQL
   비밀번호 암호화: BCrypt
   인증 방식: JWT (Access Token + Refresh Token)
2. 데이터 모델
   2-1. User 엔티티
   필드	타입	제약
   id (PK)	UUID	회원가입 시 무작위 생성, 로그인 아이디와 별개
   loginId	String	필수, UNIQUE, 4~20자, 영문 대소문자만 (숫자/특수문자 불가)
   password	String	필수, BCrypt 해시 저장. 원문 규칙: 10~22자, 특수문자 최소 1개 포함
   email	String	필수, UNIQUE
   name	String	필수
   monthlyIncome	Long	nullable. 회원가입 시점에는 입력받지 않음 — 별도 "데이터 연동" 화면에서 입력
   createdAt	DateTime	자동 생성
   updatedAt	DateTime	자동 갱신

로그인은 loginId + password 조합으로 처리한다. loginId, email 각각 DB 레벨 UNIQUE 제약을 건다.

2-2. MyDataSnapshot 엔티티 (마이데이터 연동 결과)
사용자당 최신 스냅샷 1개만 유지한다 (재연동 시 기존 레코드 UPDATE, 이력 누적 저장 아님).
User와 1:1 관계.
필드 그룹	세부 항목
소비정보 (monthlyConsumption)	food(식비), culture(문화·취미), shopping(쇼핑), etc(기타소비), fixedCost(고정비용 — 하위에 transport/telecom/insurance/subscription/loanInterest/housing 포함)
자산정보 (asset)	deposit(예금), saving(적금), investment(투자자산), loan(대출), remainingRepayment(남은 상환금액)
3. 마이데이터 연동 처리 방식
   사용자가 직접 입력하는 것이 아니라, 백엔드가 mock 데이터를 자체 생성해서 저장한다.
   mock 데이터의 정확한 JSON 스키마(필드명, 타입, 값 범위)는 개발자가 직접 설계한다. 설계 완료 후 스키마를 별도로 보고할 것.
   스키마 설계 시 위 2-2 항목의 카테고리 구조(소비정보/자산정보 하위 필드)를 반드시 반영해야 한다.
   연동 API 호출 시마다 기존 스냅샷을 덮어쓴다 (upsert).
4. 인증 흐름
   4-1. 회원가입
   필수값: loginId, password, email, name (전부 필수, 하나라도 누락 시 에러)
   유효성 검증 순서: 형식 검증(길이/문자규칙) → loginId 중복확인 → email 중복확인
   password는 BCrypt로 해시하여 저장, 원문은 저장하지 않음
   성공 시 UUID(PK) 자동 발급
   4-2. 로그인
   loginId + password로 인증
   성공 시 Access Token, Refresh Token 함께 발급
   4-3. JWT 정책
   항목	값
   Access Token 만료	24시간
   Refresh Token	사용함 (별도 만료 정책 필요 시 추가 협의)
   토큰 전달 방식	응답 헤더 (Authorization: Bearer {token})

Refresh Token을 이용한 재발급 엔드포인트(/api/auth/reissue 등)를 별도로 구현할 것.

5. 공통 오류 응답 양식

전체 API에 아래 형식을 통일 적용한다.

json
{
"success": false,
"errorCode": "AUTH_001",
"message": "이미 존재하는 아이디입니다.",
"timestamp": "2026-07-30T10:00:00Z"
}
errorCode는 도메인별 접두어 + 일련번호 규칙으로 관리 (예: AUTH_001, AUTH_002, MYDATA_001 ...)
성공 응답도 동일하게 success: true 필드를 포함하는 통일된 wrapper 구조를 사용할 것 (예: { "success": true, "data": {...} })
6. 필드 유효성 규칙 요약
   필드	규칙
   loginId	4~20자, 영문 대소문자만 (숫자·특수문자·공백 불가)
   password	10~22자, 특수문자 최소 1개 포함
   email	이메일 형식 검증 필요 (형식 규칙은 표준 이메일 정규식 사용)
7. 구현 범위에서 제외되는 것 (확인된 사항)
   마이데이터는 프론트 입력이 아닌 백엔드 자체 mock 생성 — 외부 API 연동 로직 불필요 (현 단계)
   회원가입 시 월 평균 소득 입력 없음 — 별도 API로 분리
   마이데이터 이력 저장 불필요 — 최신 스냅샷만 관리