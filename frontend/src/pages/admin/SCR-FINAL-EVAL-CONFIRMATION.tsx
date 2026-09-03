import type React from "react";
import { BadgeCheck, RotateCcw, Search, XCircle } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  finalEvaluationConfirmationApi,
  type ApiErrorField,
  type FinalEvaluationActionType,
  type FinalEvaluationConfirmationRow,
  type FinalEvaluationStatus,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FinalEvaluationFilters = {
  evaluationYear: string;
  targetUserId: string;
  finalStatus: "" | FinalEvaluationStatus;
  actionType: FinalEvaluationActionType;
  cancelReason: string;
  reason: string;
};

const initialFilters: FinalEvaluationFilters = {
  evaluationYear: "",
  targetUserId: "",
  finalStatus: "",
  actionType: "CONFIRM",
  cancelReason: "",
  reason: "",
};

export function FinalEvaluationConfirmationPage() {
  const [filters, setFilters] =
    useState<FinalEvaluationFilters>(initialFilters);
  const [rows, setRows] = useState<FinalEvaluationConfirmationRow[]>([]);
  const [selectedTargetId, setSelectedTargetId] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [lastBatchId, setLastBatchId] = useState<string | null>(null);

  const selectedRow = useMemo(
    () =>
      rows.find((row) => row.targetUserId === selectedTargetId) ??
      rows[0] ??
      null,
    [rows, selectedTargetId],
  );

  const conditionSummary = useMemo(
    () =>
      `평가연도 ${filters.evaluationYear || "선택 대상 기준"} / 대상자 ${(selectedRow?.targetUserId ?? filters.targetUserId) || "미선택"} / 확정상태 ${filters.finalStatus || "전체"}`,
    [filters, selectedRow],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await finalEvaluationConfirmationApi.listFinalEvaluationConfirmations({
          evaluationYear: filters.evaluationYear,
          targetUserId: parseOptionalNumber(filters.targetUserId),
          finalStatus: filters.finalStatus,
          page,
          size: pageSize,
        });
      const confirmations = response.data?.confirmations ?? [];
      setRows(confirmations);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelectedTargetId(
        (current) => current ?? confirmations[0]?.targetUserId ?? null,
      );
      setFieldErrors({});
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const search = async () => {
    setPage(0);
    await load();
  };

  const reset = () => {
    setFilters(initialFilters);
    setSelectedTargetId(null);
    setFieldErrors({});
    setError(null);
    setSuccessMessage(null);
    setLastBatchId(null);
  };

  const transition = async (actionType: FinalEvaluationActionType) => {
    const targetId =
      selectedRow?.targetUserId ?? parseOptionalNumber(filters.targetUserId);
    const clientErrors = validate(filters, actionType, targetId);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    const confirmText =
      actionType === "CONFIRM"
        ? `${conditionSummary} 대상 최종평가를 확정하시겠습니까?`
        : `${conditionSummary} 대상 최종평가 확정을 취소하시겠습니까?`;
    if (!window.confirm(confirmText)) {
      return;
    }
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response =
        await finalEvaluationConfirmationApi.saveFinalEvaluationConfirmationTransition(
          targetId!,
          {
            actionType,
            evaluationYear:
              filters.evaluationYear || selectedRow?.evaluationYear,
            cancelReason:
              actionType === "CANCEL" ? filters.cancelReason.trim() : undefined,
            reason:
              actionType === "CONFIRM" ? filters.reason.trim() : undefined,
          },
        );
      const result = response.data;
      setLastBatchId(result?.finalizationBatchId ?? null);
      setSuccessMessage(
        result
          ? `${actionType === "CONFIRM" ? "평가 확정" : "확정취소"} 완료: 배치ID ${result.finalizationBatchId}, 총 ${result.totalCount}건 / 성공 ${result.successCount}건 / 제외 ${result.excludedCount}건, snapshot ${result.snapshotRef}`
          : "평가 확정·취소 처리가 완료되었습니다.",
      );
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "평가 확정·취소 처리를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-FINAL-EVAL-CONFIRMATION"
        data-testid="final-evaluation-confirmation-page"
      >
        <PermissionState
          title="평가 확정·취소 권한이 없습니다"
          message="R09 또는 평가 확정 실행 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-FINAL-EVAL-CONFIRMATION"
      data-testid="final-evaluation-confirmation-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 일괄처리 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              평가 확정·취소
            </h1>
            <p className="mt-2 text-sm text-muted">
              대상자별 최종평가 상태를 조회하고 확정 snapshot 또는 취소사유를
              보존합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="final-evaluation-refresh-button"
          >
            <RotateCcw size={16} />
            새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="처리 후 같은 조건으로 확정상태·처리자·사유를 재조회했습니다."
        />
      ) : null}
      {error ? (
        <ErrorState title="평가 확정·취소 오류" message={error} />
      ) : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">최종평가 조회 조건</h2>
        <p className="mt-2 text-sm text-muted">
          실행 전 확인 조건: {conditionSummary}
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Field label="평가연도 *" error={fieldErrors.evaluationYear}>
            <input
              className="form-input"
              value={filters.evaluationYear}
              onChange={update("evaluationYear")}
              data-testid="final-evaluation-year-input"
            />
          </Field>
          <Field
            label="대상자 ID"
            error={fieldErrors.targetId ?? fieldErrors.targetUserId}
          >
            <input
              className="form-input"
              value={filters.targetUserId}
              onChange={update("targetUserId")}
              data-testid="final-evaluation-target-user-input"
            />
          </Field>
          <Field label="확정상태" error={fieldErrors.finalStatus}>
            <select
              className="form-select"
              value={filters.finalStatus}
              onChange={updateSelect("finalStatus")}
              data-testid="final-evaluation-status-select"
            >
              <option value="">전체</option>
              <option value="CERTIFIED">인증</option>
              <option value="EVALUATION_CONFIRMED">평가확정</option>
            </select>
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="final-evaluation-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={reset}
            data-testid="final-evaluation-reset-button"
          >
            입력 초기화
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              대상자별 최종평가 상태
            </h2>
            {lastBatchId ? (
              <p className="mt-1 text-sm text-success">
                최근 확정처리 배치ID: {lastBatchId}
              </p>
            ) : null}
          </div>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="form-select ml-2"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as PageSize)
              }
              data-testid="final-evaluation-page-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="최종평가 상태를 불러오는 중입니다" />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 최종평가 대상이 없습니다" />
        ) : null}
        <div className="mt-4 overflow-x-auto">
          <table
            className="min-w-full divide-y divide-ld text-sm"
            data-testid="final-evaluation-target-table"
          >
            <thead className="bg-lightgray text-left text-muted">
              <tr>
                <th className="px-4 py-3">선택</th>
                <th className="px-4 py-3">대상자</th>
                <th className="px-4 py-3">최종점수</th>
                <th className="px-4 py-3">최신재계산</th>
                <th className="px-4 py-3">확정상태</th>
                <th className="px-4 py-3">확정자/일시</th>
                <th className="px-4 py-3">취소사유</th>
                <th className="px-4 py-3">snapshot</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={`${row.evaluationYear}-${row.targetUserId}`}
                  data-testid="final-evaluation-target-row"
                >
                  <td className="px-4 py-3">
                    <input
                      type="radio"
                      name="final-evaluation-target"
                      checked={
                        (selectedTargetId ?? rows[0]?.targetUserId) ===
                        row.targetUserId
                      }
                      onChange={() => setSelectedTargetId(row.targetUserId)}
                      data-testid="final-evaluation-target-radio"
                    />
                  </td>
                  <td className="px-4 py-3">{row.targetUserId}</td>
                  <td className="px-4 py-3">{formatScore(row.finalScore)}</td>
                  <td className="px-4 py-3">
                    {row.latestRecalculationBatchId ?? "-"} /{" "}
                    {row.latestRecalculationStatus}
                  </td>
                  <td className="px-4 py-3">{statusLabel(row.finalStatus)}</td>
                  <td className="px-4 py-3">
                    {row.confirmedBy
                      ? `${row.confirmedBy} / ${formatDate(row.confirmedAt)}`
                      : "-"}
                  </td>
                  <td className="px-4 py-3">{row.cancelReason ?? "-"}</td>
                  <td className="px-4 py-3">{row.snapshotRef ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-4 flex items-center justify-between text-sm text-muted">
          <span>총 {totalElements}건</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => Math.max(value - 1, 0))}
            data-testid="final-evaluation-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => value + 1)}
            data-testid="final-evaluation-next-page-button"
          >
            다음
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">확정·취소 처리</h2>
        <p className="mt-2 text-sm text-muted">
          점수와 산출근거는 이 화면에서 직접 수정하지 않고, 확정 snapshot과
          취소사유만 보존합니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Field label="처리구분 *" error={fieldErrors.actionType}>
            <select
              className="form-select"
              value={filters.actionType}
              onChange={updateSelect("actionType")}
              data-testid="final-evaluation-action-type-select"
            >
              <option value="CONFIRM">CONFIRM</option>
              <option value="CANCEL">CANCEL</option>
            </select>
          </Field>
          <Field label="확정 사유" error={fieldErrors.reason}>
            <input
              className="form-input"
              value={filters.reason}
              onChange={update("reason")}
              data-testid="final-evaluation-reason-input"
            />
          </Field>
          <Field label="취소사유 *" error={fieldErrors.cancelReason}>
            <input
              className="form-input"
              value={filters.cancelReason}
              onChange={update("cancelReason")}
              data-testid="final-evaluation-cancel-reason-input"
            />
          </Field>
        </div>
        {fieldErrors.permission ? (
          <p className="mt-3 text-sm text-error">{fieldErrors.permission}</p>
        ) : null}
        {fieldErrors.recalculationStatus ? (
          <p className="mt-3 text-sm text-error">
            {fieldErrors.recalculationStatus}
          </p>
        ) : null}
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-success px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void transition("CONFIRM")}
            disabled={saving}
            data-testid="final-evaluation-confirm-button"
          >
            <BadgeCheck size={16} />
            {saving ? "처리 중" : "확정"}
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-error px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void transition("CANCEL")}
            disabled={saving}
            data-testid="final-evaluation-cancel-button"
          >
            <XCircle size={16} />
            {saving ? "처리 중" : "확정취소"}
          </button>
        </div>
      </section>
    </section>
  );

  function update(field: keyof FinalEvaluationFilters) {
    return (event: React.ChangeEvent<HTMLInputElement>) => {
      setFilters((current) => ({ ...current, [field]: event.target.value }));
      setFieldErrors({});
      setSuccessMessage(null);
    };
  }

  function updateSelect(field: "finalStatus" | "actionType") {
    return (event: React.ChangeEvent<HTMLSelectElement>) => {
      setFilters((current) => ({
        ...current,
        [field]: event.target.value as FinalEvaluationFilters[typeof field],
      }));
      setFieldErrors({});
      setSuccessMessage(null);
    };
  }
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <div className="mt-1">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function validate(
  filters: FinalEvaluationFilters,
  actionType: FinalEvaluationActionType,
  targetId?: number,
) {
  const errors: Record<string, string> = {};
  if (!targetId)
    errors.targetId = "처리할 대상자를 선택하거나 대상자 ID를 입력하세요.";
  if (
    filters.targetUserId.trim() &&
    Number.isNaN(Number(filters.targetUserId.trim()))
  ) {
    errors.targetUserId = "대상자 ID는 숫자로 입력하세요.";
  }
  if (actionType === "CANCEL" && !filters.cancelReason.trim()) {
    errors.cancelReason = "확정취소 사유를 입력하세요.";
  }
  return errors;
}

function parseOptionalNumber(value: string) {
  return value.trim() ? Number(value.trim()) : undefined;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function formatScore(value: number) {
  return Number(value ?? 0).toLocaleString("ko-KR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatDate(value?: string | null) {
  return value ? value.replace("T", " ").slice(0, 16) : "-";
}

function statusLabel(status: string) {
  return status === "EVALUATION_CONFIRMED" ? "평가확정" : "인증";
}
