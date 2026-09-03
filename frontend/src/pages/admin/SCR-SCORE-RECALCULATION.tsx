import type React from "react";
import { Calculator, RefreshCw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  scoreRecalculationApi,
  type ApiErrorField,
  type PageSize,
  type ScoreRecalculationRow,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type RecalculationFilters = {
  evaluationYear: string;
  areaCode: string;
  targetUserId: string;
  formulaVersionId: string;
  selectionReason: string;
};

const initialFilters: RecalculationFilters = {
  evaluationYear: "",
  areaCode: "",
  targetUserId: "",
  formulaVersionId: "",
  selectionReason: "",
};

export function ScoreRecalculationPage() {
  const [filters, setFilters] = useState<RecalculationFilters>(initialFilters);
  const [rows, setRows] = useState<ScoreRecalculationRow[]>([]);
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

  const conditionSummary = useMemo(
    () =>
      `평가연도 ${filters.evaluationYear || "-"} / 영역 ${filters.areaCode || "-"} / 대상자 ${filters.targetUserId || "전체"} / 산식버전 ${filters.formulaVersionId || "-"}`,
    [filters],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await scoreRecalculationApi.listScoreRecalculations({
        evaluationYear: filters.evaluationYear,
        areaCode: filters.areaCode,
        targetUserId: parseOptionalNumber(filters.targetUserId),
        page,
        size: pageSize,
      });
      setRows(response.data?.recalculations ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
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
    setFieldErrors({});
    setError(null);
    setSuccessMessage(null);
    setLastBatchId(null);
  };

  const recalculate = async () => {
    const clientErrors = validate(filters);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (
      !window.confirm(`${conditionSummary} 조건으로 점수를 재계산하시겠습니까?`)
    ) {
      return;
    }
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await scoreRecalculationApi.createScoreRecalculation({
        evaluationYear: filters.evaluationYear.trim(),
        areaCode: filters.areaCode.trim(),
        targetUserId: parseOptionalNumber(filters.targetUserId),
        formulaVersionId: filters.formulaVersionId.trim(),
        selectionReason: filters.selectionReason.trim(),
      });
      const result = response.data;
      setLastBatchId(result?.recalculationBatchId ?? null);
      setSuccessMessage(
        result
          ? `점수 재계산 완료: 재계산배치ID ${result.recalculationBatchId}, 총 ${result.totalCount}건 / 성공 ${result.successCount}건 / 실패 ${result.failureCount}건 / 제외 ${result.excludedCount}건`
          : "점수 재계산이 완료되었습니다.",
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
        : "점수 재계산 처리를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-SCORE-RECALCULATION"
        data-testid="score-recalculation-page"
      >
        <PermissionState
          title="점수 재계산 권한이 없습니다"
          message="R09 또는 점수 재계산 실행 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-SCORE-RECALCULATION"
      data-testid="score-recalculation-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 일괄처리 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              점수 재계산
            </h1>
            <p className="mt-2 text-sm text-muted">
              평가자료 점수를 선택한 산식버전으로 재계산하고 변경 전후 값을
              비교합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="score-recalculation-refresh-button"
          >
            <RefreshCw size={16} />
            새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="재계산 후 같은 조건으로 전후 점수와 산식버전을 재조회했습니다."
        />
      ) : null}
      {error ? <ErrorState title="점수 재계산 오류" message={error} /> : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">
          재계산 조건 및 산식버전
        </h2>
        <p className="mt-2 text-sm text-muted">
          실행 전 확인 조건: {conditionSummary}
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-5">
          <Field label="평가연도 *" error={fieldErrors.evaluationYear}>
            <input
              className="form-input"
              value={filters.evaluationYear}
              onChange={update("evaluationYear")}
              data-testid="score-recalculation-year-input"
            />
          </Field>
          <Field label="평가영역 *" error={fieldErrors.areaCode}>
            <input
              className="form-input"
              value={filters.areaCode}
              onChange={update("areaCode")}
              data-testid="score-recalculation-area-input"
            />
          </Field>
          <Field label="대상자 ID" error={fieldErrors.targetUserId}>
            <input
              className="form-input"
              value={filters.targetUserId}
              onChange={update("targetUserId")}
              data-testid="score-recalculation-target-user-input"
            />
          </Field>
          <Field label="산식버전 *" error={fieldErrors.formulaVersionId}>
            <input
              className="form-input"
              value={filters.formulaVersionId}
              onChange={update("formulaVersionId")}
              data-testid="score-recalculation-formula-version-input"
            />
          </Field>
          <Field label="선택 사유 *" error={fieldErrors.selectionReason}>
            <input
              className="form-input"
              value={filters.selectionReason}
              onChange={update("selectionReason")}
              data-testid="score-recalculation-selection-reason-input"
            />
          </Field>
        </div>
        <p className="mt-3 text-sm text-muted">
          안내: 산식 정의와 원천 실적은 이 화면에서 수정하지 않습니다.
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="score-recalculation-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={reset}
            data-testid="score-recalculation-reset-button"
          >
            초기화
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-success px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void recalculate()}
            disabled={saving}
            data-testid="score-recalculation-execute-button"
          >
            <Calculator size={16} />
            {saving ? "재계산 중" : "재계산 실행"}
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              재계산 대상 및 전후 비교
            </h2>
            {lastBatchId ? (
              <p className="mt-1 text-sm text-success">
                최근 재계산배치ID: {lastBatchId}
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
              data-testid="score-recalculation-page-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="점수 재계산 대상을 불러오는 중입니다" />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 점수 재계산 대상이 없습니다" />
        ) : null}
        <div className="mt-4 overflow-x-auto">
          <table
            className="min-w-full divide-y divide-ld text-sm"
            data-testid="score-recalculation-target-table"
          >
            <thead className="bg-lightgray text-left text-muted">
              <tr>
                <th className="px-4 py-3">평가자료ID</th>
                <th className="px-4 py-3">대상자</th>
                <th className="px-4 py-3">평가영역</th>
                <th className="px-4 py-3">이전점수</th>
                <th className="px-4 py-3">재계산점수</th>
                <th className="px-4 py-3">산식버전</th>
                <th className="px-4 py-3">계산세대</th>
                <th className="px-4 py-3">재계산배치ID</th>
                <th className="px-4 py-3">제외 사유</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr
                  key={row.evaluationMaterialId}
                  data-testid="score-recalculation-target-row"
                >
                  <td className="px-4 py-3">{row.evaluationMaterialId}</td>
                  <td className="px-4 py-3">{row.targetUserId}</td>
                  <td className="px-4 py-3">{row.areaCode}</td>
                  <td className="px-4 py-3">
                    {formatScore(row.previousScore)}
                  </td>
                  <td className="px-4 py-3">
                    {formatScore(row.recalculatedScore)}
                  </td>
                  <td className="px-4 py-3">{row.formulaVersionId ?? "-"}</td>
                  <td className="px-4 py-3">{row.generationNo ?? "-"}</td>
                  <td className="px-4 py-3">
                    {row.recalculationBatchId ?? "-"}
                  </td>
                  <td className="px-4 py-3">{row.excludedReason ?? "-"}</td>
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
            data-testid="score-recalculation-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => value + 1)}
            data-testid="score-recalculation-next-page-button"
          >
            다음
          </button>
        </div>
      </section>
    </section>
  );

  function update(field: keyof RecalculationFilters) {
    return (event: React.ChangeEvent<HTMLInputElement>) => {
      setFilters((current) => ({ ...current, [field]: event.target.value }));
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

function validate(filters: RecalculationFilters) {
  const errors: Record<string, string> = {};
  if (!filters.evaluationYear.trim())
    errors.evaluationYear = "평가연도를 입력하세요.";
  if (!filters.areaCode.trim()) errors.areaCode = "평가영역을 입력하세요.";
  if (!filters.formulaVersionId.trim())
    errors.formulaVersionId = "산식버전을 입력하세요.";
  if (!filters.selectionReason.trim())
    errors.selectionReason = "선택 사유를 입력하세요.";
  if (
    filters.targetUserId.trim() &&
    Number.isNaN(Number(filters.targetUserId.trim()))
  ) {
    errors.targetUserId = "대상자 ID는 숫자로 입력하세요.";
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
