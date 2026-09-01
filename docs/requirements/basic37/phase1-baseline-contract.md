# BASIC-37 Phase 1 Baseline Contract Setup

## Scope

- T001: `.specify/specs/001-feature/requirement-registry.json` now maps REQ-1149 through REQ-1161 to implementation issue labels and durable test labels.
- T002: Approved effective OpenAPI, data-model, and UI design contracts are materialized under `docs/requirements/basic37/effective-contract/`; backend main/test OpenAPI resources match that effective OpenAPI.

## Behavior to preserve

- Existing Spring Boot 3.3.x, Java 17, Maven, PostgreSQL 16, React 18, Vite 5, npm, and Docker Compose stack contracts remain unchanged.
- Existing `SessionCookie`, `ApiResponse`, and `ApiError` envelope semantics remain the baseline for later API implementation phases.
- Phase 2 seed work and Phase 3+ API/UI implementation are intentionally out of scope for this phase.

## Requested change enabled

- BASIC-37 requirement traceability is now explicit before subsequent RED/GREEN implementation phases.
- The backend contract fixture includes the approved BASIC-37 operation traces for researcher profile queries, batch result queries, and upload template queries.

## Verification evidence

Command:

```bash
bash tests/smoke/basic37-phase1-baseline.sh
```

Expected result after this phase:

```text
PASS basic37 phase1 baseline contract setup
```

Long-running Maven/npm test commands were not run during codegen, per the run contract.
