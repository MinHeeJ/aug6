import type React from "react";
import { Calculator, RefreshCw, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  scoreRecalculationApi,
  type ApiErrorField,
  type PageSize,
  type ScoreRecalculationResult,
  type ScoreRecalculationTarget,
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
  formulaVersionId: string;
};

const initialFilters: Filters = {
  evaluationYear: "2026",
  areaCode: "",
  targetUserId: "",
  formulaVersionId: "",
};

export function ScoreRecalculationPage() {
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [targets, setTargets] = useState<ScoreRecalculationTarget[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [result, setResult] = useState<ScoreRecalculationResult | null>(null);

  const preview = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await scoreRecalculationApi.preview({
        ...filters,
        page,
        size: pageSize,
      });
      setTargets(response.data?.targets ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void preview();
  }, [page, pageSize]);

  const recalculate = async () => {
    const clientErrors = validateFilters(filters);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (
      !window.confirm(
        "미리보기 조건의 평가자료 점수를 선택한 산식버전으로 재계산하시겠습니까?",
      )
    ) {
      return;
    }
    try {
      setRunning(true);
      setError(null);
      setFieldErrors({});
      const response = await scoreRecalculationApi.recalculate(filters);
      setResult(response.data ?? null);
      await preview();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setRunning(false);
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
        : "점수 재계산을 처리하지 못했습니다.",
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
          message="R09 업무담당 권한 또는 메뉴 접근 권한이 필요합니다."
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
              평가연도·대상자·영역과 산식버전을 지정하여 전후 점수를 비교하고
              선택 대상만 재계산합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={() => void preview()}
              data-testid="score-recalculation-preview-button"
            >
              <Search size={16} /> 전후 비교
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              onClick={() => void recalculate()}
              disabled={running}
              data-testid="score-recalculation-run-button"
            >
              <Calculator size={16} /> {running ? "재계산 중" : "재계산 실행"}
            </button>
          </div>
        </div>
      </div>

      {result ? (
        <SuccessState
          title="점수 재계산 요청 완료"
          message={`batchId ${result.batchId} / 대상 ${result.targetCount}건 / 재계산 ${result.recalculatedCount}건 / 제외 ${result.excludedCount}건`}
        />
      ) : null}
      {result ? (
        <a
          className="inline-flex rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
          href="/admin/evaluation-batch-results"
          data-testid="score-recalculation-result-link"
        >
          처리 결과 조회로 이동
        </a>
      ) : null}
      {error ? <ErrorState title="점수 재계산 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-6 shadow-md"
        data-testid="score-recalculation-filter-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          <Field label="평가연도" error={fieldErrors.evaluationYear} required>
            <input
              id="scoreRecalculationEvaluationYear"
              className="field-input"
              value={filters.evaluationYear}
              onChange={(event) =>
                setFilters({ ...filters, evaluationYear: event.target.value })
              }
              data-testid="score-recalculation-year-input"
            />
          </Field>
          <Field label="평가영역">
            <input
              id="scoreRecalculationAreaCode"
              className="field-input"
              value={filters.areaCode}
              onChange={(event) =>
                setFilters({ ...filters, areaCode: event.target.value })
              }
              placeholder="EDUCATION"
              data-testid="score-recalculation-area-input"
            />
          </Field>
          <Field label="대상자">
            <input
              id="scoreRecalculationTargetUserId"
              className="field-input"
              value={filters.targetUserId}
              onChange={(event) =>
                setFilters({ ...filters, targetUserId: event.target.value })
              }
              placeholder="user_id"
              data-testid="score-recalculation-target-user-input"
            />
          </Field>
          <Field label="산식버전" error={fieldErrors.formulaVersionId} required>
            <input
              id="scoreRecalculationFormulaVersionId"
              className="field-input"
              value={filters.formulaVersionId}
              onChange={(event) =>
                setFilters({ ...filters, formulaVersionId: event.target.value })
              }
              placeholder="formula_version_id"
              data-testid="score-recalculation-formula-version-input"
            />
          </Field>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">재계산 전후 비교</h2>
          <button
            type="button"
            className="inline-flex items-center gap-2 text-sm font-semibold text-primary"
            onClick={() => void preview()}
            data-testid="score-recalculation-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
        {loading ? (
          <LoadingState
            title="재계산 대상 조회 중"
            message="평가자료와 산식버전 기준 전후 점수를 비교합니다."
          />
        ) : null}
        {!loading && targets.length === 0 ? (
          <EmptyState
            title="재계산 대상이 없습니다"
            message="평가연도·영역·대상자·산식버전 조건을 변경해 보세요."
          />
        ) : null}
        {!loading && targets.length > 0 ? (
          <div className="overflow-x-auto">
            <table
              className="min-w-full divide-y divide-ld text-left text-sm"
              data-testid="score-recalculation-table"
            >
              <thead className="bg-lightsecondary text-xs uppercase text-muted">
                <tr>
                  <th className="px-4 py-3">자료ID</th>
                  <th className="px-4 py-3">평가연도</th>
                  <th className="px-4 py-3">영역</th>
                  <th className="px-4 py-3">대상자</th>
                  <th className="px-4 py-3">산식</th>
                  <th className="px-4 py-3">재계산 전</th>
                  <th className="px-4 py-3">재계산 후</th>
                  <th className="px-4 py-3">다음 세대</th>
                  <th className="px-4 py-3">제목</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {targets.map((target) => (
                  <tr
                    key={target.evaluationMaterialId}
                    data-testid="score-recalculation-row"
                  >
                    <td className="px-4 py-3 font-semibold text-primary">
                      {target.evaluationMaterialId}
                    </td>
                    <td className="px-4 py-3">{target.evaluationYear}</td>
                    <td className="px-4 py-3">{target.areaCode}</td>
                    <td className="px-4 py-3">{target.targetUserId}</td>
                    <td className="px-4 py-3">{target.formulaCode}</td>
                    <td className="px-4 py-3">
                      {formatScore(target.beforeScore)}
                    </td>
                    <td className="px-4 py-3 font-semibold text-success">
                      {formatScore(target.afterScore)}
                    </td>
                    <td className="px-4 py-3">{target.nextGenerationNo}</td>
                    <td className="px-4 py-3">{target.achievementTitle}</td>
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
            data-testid="score-recalculation-size-select"
          >
            <option value={20}>20건</option>
            <option value={50}>50건</option>
            <option value={100}>100건</option>
          </select>
        </div>
      </section>
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
  if (!filters.formulaVersionId.trim())
    errors.formulaVersionId = "산식버전을 선택하세요.";
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
