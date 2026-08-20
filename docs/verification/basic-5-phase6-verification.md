# BASIC-5 aug6 Phase 6 검증 보고서

본 문서는 `.aiops-spec/tasks.md`의 Phase 6 항목 T016~T018에 대한 정적 검증 결과를 저장한 산출물이다. `.aiops-spec/` 입력은 읽기 전용 경계를 준수하여 수정하지 않았다.

## 검증 범위

| task | canonical_id | 검증 항목 | 결과 |
|---|---|---|---|
| T016 | REQ-293, REQ-295 | design-doc-change-set.json 문서 목록, patch hash, diff dry-run, effective 문서 일치 | PASS, 단 OpenAPI patch_ref 표기 경고 있음 |
| T017 | REQ-294, REQ-303, REQ-304, REQ-305 | spec/plan/tasks/registry 요구 회계 row 수와 canonical_id 일치 | PASS for spec/tasks/registry, WARNING for plan.md legacy ids |
| T018 | REQ-291, REQ-292 | 한국어 본문과 식별자 원문 유지 | PASS |

## T016: design-doc-change-set.json 및 diff dry-run

### 문서 목록 검증

`design-doc-change-set.json`의 문서 수는 6건이다.

| path | status | patch_ref | 검증 결과 |
|---|---|---|---|
| data-model.md | changed | design-doc-patches/data-model.diff | 존재 및 dry-run 적용 성공 |
| contracts/openapi.yaml | changed | design-doc-patches/contracts-openapi.diff | 경고: 표기 경로는 없음. 실제 staged patch는 design-doc-patches/contracts/openapi.diff |
| research.md | reference_only | 없음 | baseline/effective 동일 |
| quickstart.md | changed | design-doc-patches/quickstart.diff | 존재 및 dry-run 적용 성공 |
| architecture.md | changed | design-doc-patches/architecture.diff | 존재 및 dry-run 적용 성공 |
| ui-design.md | changed | design-doc-patches/ui-design.diff | 존재 및 dry-run 적용 성공 |

### approved-change-set hash 검증

`approved-change-set.json`의 `change_set_sha256`은 staged `design-doc-change-set.json`의 실제 SHA-256과 일치한다.

| artifact | declared SHA-256 일치 여부 |
|---|---|
| design-doc-change-set.json | 일치 |
| data-model.md baseline/effective | 일치 |
| contracts/openapi.yaml baseline/effective | 일치 |
| research.md baseline/effective | 일치 |
| quickstart.md baseline/effective | 일치 |
| architecture.md baseline/effective | 일치 |
| ui-design.md baseline/effective | 일치 |
| staged patch files | declared patch SHA-256과 모두 일치 |

### diff dry-run 결과

Staged patch 파일을 canonical-design-docs 사본에 `git apply --check` 방식으로 dry-run 적용한 뒤 실제 적용 결과를 effective-design-docs와 비교했다.

| document | dry-run | applied output equals effective document |
|---|---|---|
| data-model.md | PASS | PASS |
| contracts/openapi.yaml | PASS | PASS |
| quickstart.md | PASS | PASS |
| architecture.md | PASS | PASS |
| ui-design.md | PASS | PASS |
| research.md | N/A(reference_only) | PASS(baseline == effective) |

경고: `design-doc-change-set.json`의 OpenAPI `patch_ref`는 `design-doc-patches/contracts-openapi.diff`로 표기되어 있으나 staged 파일은 `design-doc-patches/contracts/openapi.diff`에 존재한다. `approved-change-set.json`의 patch SHA-256은 staged `design-doc-patches/contracts/openapi.diff`와 일치하므로 검증은 staged patch를 기준으로 수행했다.

## T017: 요구 회계 row 수 및 canonical_id 일치

### 핵심 ledger 비교

| source | row count | unique canonical_id count | 비고 |
|---|---:|---:|---|
| spec.md Requirement Registry | 54 | 46 | 여러 Registry table에서 같은 canonical_id가 반복 참조된다. unique set은 요구 회계와 일치 |
| spec.md 요구 회계 | 46 | 46 | 기준 set |
| tasks.md 요구 회계 | 46 | 46 | spec.md 요구 회계와 일치 |
| plan.md 요구 회계 | 37 | 37 | 경고: legacy canonical_id 체계 사용 |

### 일치 결과

| 비교 | 결과 |
|---|---|
| spec.md 요구 회계 vs tasks.md 요구 회계 | PASS |
| spec.md 요구 회계 vs spec.md Requirement Registry unique set | PASS |
| tasks.md Phase 6 task canonical_id 존재 여부 | PASS |
| plan.md 요구 회계 vs spec.md 요구 회계 | WARNING |

`plan.md` 요구 회계는 `REQ-113`, `REQ-114`, `REQ-115` 등 legacy canonical_id를 포함하고 있으며, `spec.md`/`tasks.md`의 현재 canonical_id set(`REQ-144`, `REQ-145`, `REQ-146`, ..., `REQ-305`)과 1:1로 일치하지 않는다. `.aiops-spec/plan.md`는 runner 소유 읽기 전용 입력이므로 본 phase에서는 수정하지 않고 차이를 검증 산출물에 명시했다.

## T018: 한국어 본문 및 식별자 원문 유지

검토 기준:

- 사용자 가시 본문은 한국어를 기본으로 유지한다.
- API path, operationId, table/file/screen id, enum 값, route 같은 식별자는 원문 형태를 유지한다.
- `.aiops-spec/` 경로는 생성된 source/test/build/runtime 파일에서 참조하지 않는다.

검토 결과:

| 항목 | 결과 | 근거 |
|---|---|---|
| 한국어 본문 | PASS | spec/tasks/plan 및 신규 UI 문구는 한국어 중심이며 고유명사성 UI 용어만 한영 혼용 |
| API path 원문 | PASS | `/api/admin/menus/usage-settings`, `/api/admin/code-groups/{groupId}/codes/usage-settings`, `/api/admin/system-settings/common`, `/api/admin/system-settings/evaluation-years` 유지 |
| operationId 원문 | PASS | listMenuUsageSettings, saveMenuUsageSettings, listDetailCodeUsageSettings, saveDetailCodeUsageSettings, getCommonSystemSettings, saveCommonSystemSettings, getEvaluationYearSettings, saveEvaluationYearSettings 유지 |
| screen_id/route 원문 | PASS | SCR-MENU-USAGE-MGMT, SCR-CODE-USAGE-MGMT, SCR-COMMON-SETTINGS-MGMT, SCR-EVALUATION-YEAR-MGMT 및 `/admin/...` route 유지 |
| table/enum 원문 | PASS | menu_usage_settings, common_system_settings, evaluation_year_settings, evaluation_year_preparations, Y/N enum 유지 |
| `.aiops-spec/` runtime 참조 | PASS | 생성 source/test/build/runtime 파일에 `.aiops-spec` 문자열 참조 없음 |

## 추가 정적 게이트

- `git diff --check`: PASS
- `docker compose -f infra/docker-compose.yml config`: PASS

장기 실행 금지 조건에 따라 `mvn test`, `npm install`, `npm run test`, dev server, watcher는 실행하지 않았다.

## 변경 파일

- `docs/verification/basic-5-phase6-verification.md`
