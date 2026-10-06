# 회원가입 최종 검증 인계

상태: awaiting runner verification. 이 CODEGEN 단계는 최종 실행 금지 지시에 따라
테스트, Maven build, npm, Docker, 네트워크 명령을 실행하지 않았다.
아래 명령은 실행 결과가 아니라 runner 실행 대상이다. PASS 영수증은 없다.

## 변경 범위와 요구 회계

- T010 / REQ-1875: `SignupFixtureAcceptanceTest`를 추가했다.
  PostgreSQL에서 testuser1/2/3의 ACTIVE/Y, 고유 lowercase email, Argon2id PHC 구조,
  독립 salt/hash, 현재 유효한 MANUAL/ACTIVE R01 단독 매핑을 조회한다.
  fixture 비밀번호는 알려져 있지 않으므로 실제 비밀번호 일치 검증을 주장하지 않는다.
- T011 / REQ-1880: `SignupSmokeAcceptanceTest`를 추가했다.
  실제 application/filter/controller/service/MyBatis HTTP 경로를 사용한다.
  AC-01 생성, AC-03 ID 충돌, AC-04 이메일 충돌, AC-05 확인 불일치,
  AC-06 약한 비밀번호, AC-07 저장 hash와 응답 비노출,
  AC-08 availability true/false, AC-09 R01 조회를 포함한다.
  missing fields, 잘못된 email/ID, 오류 시 users/user_roles/sessions 무변경,
  no-cookie/no-session 및 기존 보호 경로 401을 추가한다.
- T012 / REQ-1881: 실행은 runner로 이관한다. 기존 tasks/spec/plan 요구 회계의
  `T013 회귀/build`는 존재하지 않는 작업 참조이며 올바른 참조는 `T012`이다.
  runner 소유 입력 파일은 수정하지 않았다.
- REQ-1880의 `out-of-scope` 표기는 보안 금지 사항을 검증하지 않아도 된다는 뜻이 아니다.
  본 테스트는 관련 중복/평문/응답 비노출을 검증한다. 로그 비노출은 별도 runner 확인이 필요하다.

## 경로 ledger / 보존 경계

- POST `/api/v1/auth/signup`: `signup.SignupController.signup(SignupRequest)` →
  `SignupService.signup(SignupRequest)` (@Transactional) →
  `SignupMapper.insertUser(String, String, String): Long` →
  `mapper/signup/SignupMapper.xml` INSERT RETURNING users.user_id →
  `insertDefaultRole(Long): int` → user_roles. public userId는 users.login_id이다.
- GET `/api/v1/auth/check-userid`: `SignupController.checkUserIdAvailability(String)` →
  `SignupService.checkUserIdAvailability(String)` (readOnly) → existsLoginId.
- 두 작업은 익명 principal이며 R01은 접근 역할이 아니라 생성 계정 역할이다.
  AuthenticationFilter:35-41의 정확한 method/path 예외를 사용한다.
  역할/메뉴 검사를 우회하는 신규 broad auth prefix는 추가하지 않았다.
- 저장 식별자/키/유효기간은 V1 schema:1-15,143-161과
  V65__signup_email_and_fixtures.sql에서 확인했다.
- 기존 login/logout/me, session cookie, 보호 admin/business 동작 및 migration 원문,
  reset/OAuth2/email verification 기능은 수정하지 않았다.
- Smoke 테스트의 @Transactional은 격리/자동 정리를 위한 것이다.
  committed durability/독립 트랜잭션 rollback 증명은 기존 SignupIntegrationTest의 책임이다.

## 정적 확인으로 발견한 미해결 사항

1. AC-02 (가입 후 실제 로그인)는 차단되어 있다.
   `auth/LocalAccountAuthenticationAdapter.java:38-39`가 SHA-256 전용 비교를 유지한다.
   `signup/SignupPasswordHasher.java:9-12` 및 V65 fixture는 Argon2id를 사용한다.
   따라서 signup 성공/로그인 페이지 이동을 실제 로그인 가능의 증거로 취급하지 않는다.
   승인된 SHA-256 legacy + Argon2id 호환 정책 및 실제 signup → login → me → logout
   검증이 필요하다. 이번 verification-only 단계는 로그인 구현을 수정하지 않는다.
2. 비밀번호 범주 해석이 계층별로 다르다.
   `signup/SignupService.java:103-109`는 영문/숫자/특수문자 3종 모두를 요구한다.
   `frontend/src/pages/signup/SCR-SIGNUP.tsx:14-17,169-174`는
   대문자/소문자/숫자/특수문자 4종 중 3종을 안내한다.
   `SignupIntegrationTest.java:110-119`는 Aa123456을 거절하도록 고정한다.
   상세 원문과 pre-analysis는 4범주 중 3종 규칙을 명시한다.
   요구 해석을 확정하고 서버·UI·기존 테스트를 일치시켜야 한다.
   본 smoke는 두 해석에 공통인 약한 비밀번호 거절만 추가했으며 이 충돌을 숨기지 않는다.
3. OQ-85-01 (sys_user → users 동치) 및 OQ-85-02 (활성화/email 정비 정책)의
   사용자 확인 없이 배포 승인 완료를 주장하지 않는다.
4. 새 DB 테스트는 SPRING_DATASOURCE_URL=jdbc:postgresql:... 없으면 skipped이다.
   DB 환경이 없거나 모든 테스트가 skipped이면 PASS가 아니다.
5. protected/excluded 경로 401 검사는 익명 접근 경계를 증명할 뿐,
   OAuth2/reset/email verification 구현 전체의 무변경을 증명하지 않는다.
6. 클래스 컴파일, 실제 PostgreSQL migration, HTTP, UI/E2E, 로그 및 build 결과는 아직 없다.

## Runner 실행 대상 (미실행)

일회용 PostgreSQL 16 환경에서 정상 Spring Boot startup/Flyway를 사용한다.
DB 자격 증명을 명령 출력/보고서에 기록하지 않는다.

backend 디렉토리:

```sh
mvn -Dtest=SignupFixtureAcceptanceTest,SignupSmokeAcceptanceTest test
mvn -Dtest=SignupAnonymousAccessContractTest,SignupHttpContractRedTest,SignupOpenApiContractTest,SignupSchemaMigrationTest test
mvn -Dtest=SignupContractTest,SignupIntegrationTest,UserIdAvailabilityContractTest test
mvn -Dtest=UserIdAvailabilityHttpTest,UserIdAvailabilityPostgresTest test
mvn -Dtest=AuthenticationFlowTest,AuthContractTest,FlywaySchemaTest test
mvn -DskipTests package
```

마지막 명령은 artifact build만 확인한다. test PASS 근거로 사용하지 않는다.
전체 Maven suite는 이 단계의 실행 대상이 아니다.

frontend 디렉토리:

```sh
npm run test -- --run
npm run typecheck
npm run build
```

기존 Playwright 설정으로 tests/e2e/signup.spec.ts와 userid-availability.spec.ts를
runner가 제공하는 실행 중 app에 대해 실행한다. test 컨테이너 밖 소스 파일을
테스트 데이터로 읽거나 build 디렉토리에 복사하지 않는다.

## 이번 단계의 실제 실행 증거

- `git status --short`: git repository를 찾을 수 없어 실패했다.
  따라서 git diff에 의한 전체 회귀 범위 검증은 불가했다.
- Python acceptance Java source 포맷 검사: exit 0;
  120자 초과/줄 끝 공백 보고 없음. 이는 컴파일/테스트 증거가 아니다.
- source/contract/schema/UI 정적 읽기만 수행했다. 테스트/build 실행 결과는 없음.

## REGISTRATIONS NEEDED:

- `frontend/src/pages/LoginPage.tsx:919`의 `</form>` 직전: 기존 스타일을 재사용하는
  `회원가입` 링크를 `/signup`에 연결하고 고유 data-testid를 추가해야 한다.
  현재 파일 전체에 signup/회원가입 링크가 없으며, 기존 signup.spec.ts:18-21의
  로그인 → 회원가입 entry 시나리오는 해당 링크를 요구한다.
  SPA 방식이면 pushState와 popstate를 함께 사용해 router가 화면을 갱신하도록 한다.
- 현재 router.tsx:158-175에 익명 /signup 분기가 있고,
  apiClient.ts:1-3에 신규 API export가 존재하며 filter:35-41에 정확한 공개 예외가 있다.
  이 세 등록은 추가 수정 요청이 없다.

## 최종 통합 정합성 패스 보완

- 위 내용은 이전 verification-only 단계의 인계 기록이다.
- 미해결 사항 2의 서버·UI 범주 불일치를 수정했다. 서버도 대문자·소문자·숫자·특수문자
  4범주 중 3종 이상을 인정하며 관련 SignupContractTest/SignupIntegrationTest를 정렬했다.
- REGISTRATIONS NEEDED의 로그인 진입 링크를 LoginPage.tsx에 연결했다.
  기존 익명 /signup 분기와 pushState/popstate 흐름을 재사용하고 router 회귀 테스트를 추가했다.
- 로그인 SHA-256 전용 비교와 신규 Argon2id 저장의 충돌은 여전히 미해결이다.
  기존 로그인 변경 제외 경계를 유지했으며 별도 호환 정책 승인이 필요하다.
- 승인/effective OpenAPI 및 test resource의 ApiError.error.fields는 object이나,
  기존 production OpenAPI/ApiError.java/프론트엔드는 ValidationError 배열을 사용한다.
  공유 오류 계약 변경이나 .aiops-spec 수정은 수행하지 않았다. 계약 소유자 정리가 필요하다.
- 이 패스는 정적 대조 및 제한된 diff/공백 검사만 수행했다.
  빌드, Docker, API/DB/UI 테스트를 실행하지 않았으며 runtime PASS를 주장하지 않는다.

shared registry와 test OpenAPI fixture는 수정하지 않았다.
