# BASIC-54 Phase 2 공통 규칙 검증 Evidence

이 문서는 Phase 2(T006~T010)에서 추가한 공통 규칙 검증 항목을 durable repository evidence로 보존한다. 입력 산출물 경로를 런타임 코드나 테스트에서 참조하지 않는다.

## T006 확정 시점 데이터/양식 버전 선택

- 단건 출력: `ReportManagementCommonRulesTest.singleOutputUsesFinalSnapshotBaseDateAndApplicableFormVersionAndRecordsSuccessHistory`
  - `outputBaseDate=2026-01-01`을 서비스 입력으로 받고 `report_form_versions`의 기준일 이하 최신 버전을 선택한다.
  - 선택된 `formVersionName`, `datasetCode`, `outputBaseDate`를 응답으로 노출하고 출력 이력을 기록한다.
- 대량 출력: `ReportManagementCommonRulesTest.bulkOutputUsesSameBaseDateFormRuleBeforeCreatingQueuedJob`
  - 대량 작업 생성 전 같은 기준일 버전 식별 규칙을 적용한다.
  - 권한과 양식 버전 확인 후에만 `bulk_report_jobs`, `bulk_report_job_targets` 생성을 진행한다.

## T007 권한 차단 및 조회/실패 이력

- 서버 권한 차단: `ReportManagementCommonRulesTest.unauthorizedOutputFormatRecordsForbiddenHistoryAndDoesNotCreateFileOrBulkJob`
  - R03 PDF-only 권한으로 Excel 출력을 요청하면 `ForbiddenException`을 발생시킨다.
  - 결과 파일 참조 없이 `resultCode=FORBIDDEN` 출력 이력을 기록한다.
- API 이력 shape: `ReportManagementApiTest.listReportPrintHistoriesMasksSensitiveErrorsInForbiddenFailureHistoryShape`
  - 실패 이력 응답에 stack trace, secret, password 같은 민감 문자열을 요구하지 않는 조회 전용 shape를 고정한다.
- 클라이언트 권한 차단은 Phase 3 UI 화면 구현 시 버튼 숨김/비활성 테스트로 확장한다. Phase 2에서는 서버 차단과 실패 이력 기록을 먼저 고정했다.

## T008 목록/Excel/필수값/확인 메시지/UI 상태

- Pagination: `ReportManagementApiTest.listReportsRejectsUnsupportedPageSizeForCommonPaginationContract`
  - 허용 size는 20/50/100만 통과한다.
- 필수값 서버 검증: `ReportManagementCommonRulesTest.serverValidationRejectsMissingReportRequiredFieldsBeforeAuditMutation`
  - 필수값 누락 시 저장과 change history mutation을 수행하지 않는다.
- 필수값 API 검증: `ReportManagementApiTest.createReportOutputRejectsMissingTargetBeforeServiceMutation`
  - 출력 대상/대상 설명 누락 시 controller boundary에서 service mutation을 차단한다.
- Excel download 및 확인 메시지는 Phase 3 UI vertical slice에서 실제 화면 버튼과 flow가 생성될 때 Playwright/Vitest로 연결한다. Phase 2에서는 출력 형식 `EXCEL` 권한 차단과 size/필수값 공통 규칙을 우선 고정했다.

## T009 서버 검증/requestId/menu/seed

- `ReportManagementApiTest.createReportOutputReturnsBaseDateFormVersionAndRequestIdContract`에서 `X-Request-Id`가 응답 meta에 보존됨을 확인한다.
- `ReportManagementCommonRulesTest.seedMigrationUsesThreeCasesAndConsecutiveMenuIds`에서 다음을 검증한다.
  - 신규 report seed 3건 이상: 정상, 경계, 미사용/상태 차이 케이스
  - 대량 출력 작업/대상 seed 3건 이상: 성공, 일부실패, 진행중 케이스
  - `bulk_report_jobs`의 `request_id`, `requested_at`, `completed_at`, `total_count`와 `bulk_report_job_targets.target_person_name`을 실제 스키마 컬럼으로 보존하여 매퍼 조회 식별자와 DB 계약을 일치시킨다.
  - 메뉴 ID 560~564 연속 채번
  - `ON CONFLICT` 기반 충돌 방지
  - `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS` 기반 idempotent migration

## T010 접근성/성능/보안/개인정보/브라우저 smoke

Phase 2에서 고정한 manual/CI 체크 항목:

1. 접근성
   - Phase 3 화면 구현 시 모든 클릭/입력 요소에 `data-testid`, label, visible text를 부여한다.
   - Playwright smoke에서 `data-screen-id`와 주요 버튼 role/name을 확인한다.
2. 성능
   - API smoke는 평균 3초, 개별 5초 미만을 기준으로 측정한다.
   - MyBatis 선택 필터는 값이 있을 때만 동적 predicate를 추가하며 null-bound predicate를 사용하지 않는다.
3. 보안/개인정보
   - 출력 실패는 fileRef 없이 `FORBIDDEN` 이력으로 기록한다.
   - 민감 원문, password, secret, stack trace 문자열을 응답 본문에 노출하지 않는 검사를 유지한다.
4. 브라우저/기기
   - Phase 3 route smoke에서 desktop/tablet viewport matrix를 적용한다.
   - 기존 React shell과 Tailwind token을 재사용하고 신규 default style system은 도입하지 않는다.

## 실행 정책

- 이 Phase는 테스트와 구현 코드를 작성했지만, 요청 계약에 따라 codegen 중 `mvn test` 또는 장시간 verification command는 실행하지 않았다.
- 후속 repair/verification phase에서 권장 실행:
  - `cd backend && ./mvnw test -Dtest=ReportManagementApiTest,ReportManagementCommonRulesTest`
  - Phase 3 UI 구현 후 `cd frontend && npm run test -- --run` 및 Playwright route smoke
