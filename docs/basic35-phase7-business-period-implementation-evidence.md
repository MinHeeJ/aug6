# BASIC-35 Phase 7 — 평가·업적입력 기간 관리 Implementation Evidence

## Scope
- Implemented only Phase 7 / US5 tasks from `.aiops-spec/tasks.md`: T035-T040 and final verification/accounting tasks T041-T119.
- Source contracts used during implementation: tasks.md, spec.md, plan.md, data-model.md, contracts/openapi.yaml, ui-design.md.
- Generated source files do not reference `.aiops-spec` at runtime.

## Behavior preserved
- Existing R01-R09 role definitions and role names were not created or changed.
- Existing SessionCookie/currentUser request attribute handling, `ApiResponse`/`ApiError` envelope, `GlobalExceptionHandler`, MyBatis mapper style, and React AdminShell route pattern were reused.
- Earlier BASIC-35 screens/routes for evaluation dates, input periods, modification periods, and department chair confirm periods were not removed or weakened.
- Adjacent reference data, source data, status transitions, score rules, classification hierarchy, objection/result/exception periods, and individual achievement-entry workflows remain out of scope.

## Behavior changed
- Added backend controller/service/request/response support for `listBusinessPeriods` and `saveBusinessPeriod`:
  - GET `/api/admin/business-periods`
  - POST `/api/admin/business-periods/save`
- Added MyBatis persistence for `business_period_integrated_settings` insert/update/find/list/count/overlap checks.
- Added MockMvc/service contract tests covering pagination/filter/auth, validation, active period overlap conflict, organization data-scope block, and data_change_histories side-effect request-id propagation.
- Added frontend API client, admin route, and React screen for `/admin/business-periods` with Korean UI text, loading/empty/error/permission/success states, field-level errors, page size choices 20/50/100, Excel CTA copy, save confirmation, and success refresh.
- Added frontend route/state regression test for `SCR-BUSINESS-PERIOD-INTEGRATED-MGMT`.
- Added mandatory `.dockerignore` files for backend and frontend build contexts.

## Verification/accounting notes for T041-T119
- T041/T042: no new role definitions and no role-name changes were made.
- T043-T055: adjacent master data, source data, permissions, objection/result/exception periods, status transitions, classification, score calculation, department-chair processing, individual achievement input, faculty achievement adjudication, and academic grant adjudication were not implemented or modified.
- T056-T064: same repository/backend/frontend/infra structure was reused; existing Principal/SessionCookie, common envelope, audit history table, and common regression artifacts were preserved.
- T065-T069: unresolved external authentication/interface details, technology stack, version BOM, and existing structure were not redefined.
- T070/T071: user-facing API errors remain separated from request-id trace metadata through `ApiError` and `ApiResponse.meta.requestId`; save side effect records requestId in data_change_histories.
- T072-T078: UI keeps semantic labels/buttons, responsive grid classes, existing typography/color tokens, and compact payload; server validates all inputs and uses parameter binding.
- T079-T088: previous out-of-scope constraints for US1-US4 remain unchanged; US5 exception permissions and classification changes are excluded.
- T089-T119: canonical accounting rows are represented by this evidence file plus the added tests/source changes for REQ-1049 through REQ-1109.

## Verification performed in this phase
- Static repository inspection and source generation only.
- Per instruction, `mvn test` was not executed during codegen.
- Per instruction, package managers were not run and no lockfile was fabricated.
