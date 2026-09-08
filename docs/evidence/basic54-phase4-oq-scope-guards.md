# BASIC-54 Phase 4 OQ 및 범위 제외 Guard

## OQ 정책 격리

Reviewer 결정 전 업무 정책 값은 `report.policy.*` 설정으로 격리한다.

- `report.policy.result-file-retention-days`: 결과파일 보관기간. 기본값 `0`은 자동 만료/삭제 미적용이다.
- `report.policy.permission-merge-strategy`: 사용자/조직/역할 권한 병합 방식. 기본값 `ANY_ALLOW`는 기존 동작 보존이다.
- `report.policy.unauthorized-bulk-target-policy`: 권한 밖 대상 포함 시 처리 방식. 기본값 `REJECT_JOB`는 기존 작업 거부 경계 보존이다.
- `report.policy.record-failed-print-history`: 실패 출력 시도 이력 기록 여부. 기본값 `true`는 기존 실패 이력 기록 보존이다.

## 범위 제외 Guard

Report management slice는 다음 업무를 새 API/화면 action으로 제공하지 않는다.

- 평가 확정·취소 실행
- 시점 데이터 생성/수정
- 점수 산출·조정·재계산 실행
- 개인 업적점수·세부규정 변경
- 개별 업적 입력 저장
- 평가대상자 선정·결과 처리
- 외부 연계 실행
- 보고서 원천 평가자료 수정

Backend guard는 미매핑 report-slice API가 404를 반환하는지 확인하고, frontend guard는 report management 화면 markup에 범위 밖 action이 없는지 확인한다. 대량 출력 화면은 `bulk-oq-policy-guard` 안내 영역에서 미확정 정책 환경변수만 노출하고 결과파일 자동삭제 실행, 권한 병합 확정, 원천자료 변경 action을 제공하지 않는다.
