import type React from "react";
import { BadgeCheck, RefreshCw, RotateCcw, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  finalEvaluationConfirmationApi,
  type ApiErrorField,
  type FinalEvaluationConfirmationResult,
  type FinalEvaluationConfirmationTarget,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type Filters = {
  evaluationYear: string;
  areaCode: string;
  targetUserId: string;
  confirmationStatus: string;
};

const initialFilters: Filters = {
  evaluationYear: "2026",
  areaCode: "",
  targetUserId: "",
  confirmationStatus: "",
};

type Props = {
  currentRoles?: string[];
};

export function FinalEvaluationConfirmationPage({ currentRoles = [] }: Props) {
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [confirmations, setConfirmations] = useState<
    FinalEvaluationConfirmationTarget[]
  >([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [runningTargetId, setRunningTargetId] = useState<number | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [result, setResult] =
    useState<FinalEvaluationConfirmationResult | null>(null);
  const [cancelTarget, setCancelTarget] =
    useState<FinalEvaluationConfirmationTarget | null>(null);
  const [cancelReason, setCancelReason] = useState("");

  const canConfirm = currentRoles.some((role) => ["R04", "R09"].includes(role));
  const canCancel = currentRoles.some((role) => ["R08", "R09"].includes(role));

  const search = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await finalEvaluationConfirmationApi.list({
        ...filters,
        page,
        size: pageSize,
      });
      setConfirmations(response.data?.confirmations ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void search();
  }, [page, pageSize]);

  const confirmTarget = async (target: FinalEvaluationConfirmationTarget) => {
    const clientErrors = validateFilters(filters);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (
      !window.confirm(
        `${target.targetName} 대상자의 최종평가를 확정하시겠습니까?`,
      )
    ) {
      return;
    }
    try {
      setRunningTargetId(target.targetId);
      setError(null);
      setFieldErrors({});
      const response = await finalEvaluationConfirmationApi.confirm(
        target.targetId,
        {
          evaluationYear: filters.evaluationYear,
        },
      );
      setResult(response.data ?? null);
      await search();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setRunningTargetId(null);
    }
  };

  const submitCancel = async () => {
    if (!cancelTarget) return;
    const clientErrors = validateFilters(filters);
    if (!cancelReason.trim())
      clientErrors.cancelReason = "확정취소 사유를 입력하세요.";
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    try {
      setRunningTargetId(cancelTarget.targetId);
      setError(null);
      const response = await finalEvaluationConfirmationApi.cancel(
        cancelTarget.targetId,
        {
          evaluationYear: filters.evaluationYear,
          cancelReason,
        },
      );
      setResult(response.data ?? null);
      setCancelTarget(null);
      setCancelReason("");
      setFieldErrors({});
      await search();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setRunningTargetId(null);
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
        : "최종평가 확정 처리를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-FINAL-EVALUATION-CONFIRMATION"
        data-testid="final-evaluation-confirmation-page"
      >
        <PermissionState
          title="최종평가 확정 권한이 없습니다"
          message="R04, R08, R09 권한 또는 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-FINAL-EVALUATION-CONFIRMATION"
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
              재계산이 완료된 대상자의 최종평가 상태를 조회하고 인증 ↔ 평가확정
              전이를 처리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="final-evaluation-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </div>

      {result ? (
        <SuccessState
          title="최종평가 처리 완료"
          message={`batchId ${result.batchId} / ${result.previousStatus} → ${result.nextStatus} / 변경 ${result.changedMaterialCount}건`}
        />
      ) : null}
      {error ? <ErrorState title="최종평가 처리 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-6 shadow-md"
        data-testid="final-evaluation-filter-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          <Field label="평가연도" error={fieldErrors.evaluationYear} required>
            <input
              id="finalEvaluationYear"
              className="field-input"
              value={filters.evaluationYear}
              onChange={(event) =>
                setFilters({ ...filters, evaluationYear: event.target.value })
              }
              data-testid="final-evaluation-year-input"
            />
          </Field>
          <Field label="평가영역">
            <input
              id="finalEvaluationAreaCode"
              className="field-input"
              value={filters.areaCode}
              onChange={(event) =>
                setFilters({ ...filters, areaCode: event.target.value })
              }
              placeholder="EDUCATION"
              data-testid="final-evaluation-area-input"
            />
          </Field>
          <Field label="대상자">
            <input
              id="finalEvaluationTargetUserId"
              className="field-input"
              value={filters.targetUserId}
              onChange={(event) =>
                setFilters({ ...filters, targetUserId: event.target.value })
              }
              placeholder="user_id"
              data-testid="final-evaluation-target-user-input"
            />
          </Field>
          <Field label="확정상태">
            <select
              id="finalEvaluationStatus"
              className="field-input"
              value={filters.confirmationStatus}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  confirmationStatus: event.target.value,
                })
              }
              data-testid="final-evaluation-status-select"
            >
              <option value="">전체</option>
              <option value="인증">인증</option>
              <option value="평가확정">평가확정</option>
            </select>
          </Field>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            대상자별 최종평가 상태
          </h2>
          <button
            type="button"
            className="inline-flex items-center gap-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="final-evaluation-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
        {loading ? (
          <LoadingState
            title="최종평가 상태 조회 중"
            message="대상자별 확정자, 확정일시, 취소사유를 조회합니다."
          />
        ) : null}
        {!loading && confirmations.length === 0 ? (
          <EmptyState
            title="최종평가 대상이 없습니다"
            message="평가연도·영역·대상자 조건을 변경해 보세요."
          />
        ) : null}
        {!loading && confirmations.length > 0 ? (
          <div className="overflow-x-auto">
            <table
              className="min-w-full divide-y divide-ld text-left text-sm"
              data-testid="final-evaluation-table"
            >
              <thead className="bg-lightsecondary text-xs uppercase text-muted">
                <tr>
                  <th className="px-4 py-3">대상자</th>
                  <th className="px-4 py-3">평가연도</th>
                  <th className="px-4 py-3">영역</th>
                  <th className="px-4 py-3">조직</th>
                  <th className="px-4 py-3">상태</th>
                  <th className="px-4 py-3">점수</th>
                  <th className="px-4 py-3">확정자</th>
                  <th className="px-4 py-3">확정일시</th>
                  <th className="px-4 py-3">취소사유</th>
                  <th className="px-4 py-3">처리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {confirmations.map((target) => (
                  <tr
                    key={`${target.evaluationYear}-${target.targetId}`}
                    data-testid="final-evaluation-row"
                  >
                    <td className="px-4 py-3 font-semibold text-primary">
                      {target.targetName} ({target.targetId})
                    </td>
                    <td className="px-4 py-3">{target.evaluationYear}</td>
                    <td className="px-4 py-3">{target.areaCode}</td>
                    <td className="px-4 py-3">{target.organizationName}</td>
                    <td className="px-4 py-3">{target.confirmationStatus}</td>
                    <td className="px-4 py-3">
                      {formatScore(target.totalScore)}
                    </td>
                    <td className="px-4 py-3">
                      {target.confirmedByName ?? "-"}
                    </td>
                    <td className="px-4 py-3">{target.confirmedAt ?? "-"}</td>
                    <td className="px-4 py-3">{target.cancelReason ?? "-"}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        {canConfirm && target.confirmationStatus === "인증" ? (
                          <button
                            type="button"
                            className="inline-flex items-center gap-1 rounded-md bg-primary px-3 py-2 text-xs font-semibold text-white disabled:opacity-50"
                            onClick={() => void confirmTarget(target)}
                            disabled={runningTargetId === target.targetId}
                            data-testid="final-evaluation-confirm-button"
                          >
                            <BadgeCheck size={14} /> 확정
                          </button>
                        ) : null}
                        {canCancel &&
                        target.confirmationStatus === "평가확정" ? (
                          <button
                            type="button"
                            className="inline-flex items-center gap-1 rounded-md border border-error px-3 py-2 text-xs font-semibold text-error disabled:opacity-50"
                            onClick={() => setCancelTarget(target)}
                            disabled={runningTargetId === target.targetId}
                            data-testid="final-evaluation-cancel-button"
                          >
                            <RotateCcw size={14} /> 취소
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <div className="mt-4 flex items-center justify-between text-sm text-muted">
          <span>총 {totalElements}건</span>
          <select
            className="field-input w-28"
            value={pageSize}
            onChange={(event) => {
              setPage(0);
              setPageSize(Number(event.target.value) as PageSize);
            }}
            data-testid="final-evaluation-size-select"
          >
            <option value={20}>20건</option>
            <option value={50}>50건</option>
            <option value={100}>100건</option>
          </select>
        </div>
      </section>

      {cancelTarget ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          role="dialog"
          aria-modal="true"
          data-testid="final-evaluation-cancel-modal"
        >
          <section className="w-full max-w-lg rounded-md bg-white p-6 shadow-md">
            <h2 className="text-lg font-semibold text-dark">
              확정취소 사유 입력
            </h2>
            <p className="mt-2 text-sm text-muted">
              {cancelTarget.targetName} 대상자의 평가확정을 인증 상태로
              되돌립니다.
            </p>
            <Field label="취소사유" error={fieldErrors.cancelReason} required>
              <textarea
                className="field-input min-h-24"
                value={cancelReason}
                onChange={(event) => setCancelReason(event.target.value)}
                data-testid="final-evaluation-cancel-reason-input"
              />
            </Field>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-ld"
                onClick={() => setCancelTarget(null)}
                data-testid="final-evaluation-cancel-close-button"
              >
                닫기
              </button>
              <button
                type="button"
                className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
                onClick={() => void submitCancel()}
                data-testid="final-evaluation-cancel-submit-button"
              >
                확정취소
              </button>
            </div>
          </section>
        </div>
      ) : null}
    </section>
  );
}

function Field({
  label,
  error,
  required,
  children,
}: {
  label: string;
  error?: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <label className="block text-sm font-semibold text-ld">
      {label}
      {required ? <span className="ms-1 text-error">*</span> : null}
      <div className="mt-2">{children}</div>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function validateFilters(filters: Filters): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!filters.evaluationYear.trim())
    errors.evaluationYear = "평가연도를 입력하세요.";
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]): Record<string, string> {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function formatScore(value: number): string {
  return new Intl.NumberFormat("ko-KR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}
