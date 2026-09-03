import type React from "react";
import { FilePlus2, RefreshCw, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  evaluationMaterialGenerationApi,
  type ApiErrorField,
  type EvaluationMaterialGenerationResult,
  type EvaluationMaterialGenerationTarget,
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
  organizationCode: string;
  targetUserId: string;
};

const initialFilters: Filters = {
  evaluationYear: "2026",
  areaCode: "RESEARCH_CREATION",
  organizationCode: "",
  targetUserId: "",
};

export function EvaluationMaterialGenerationPage() {
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [targets, setTargets] = useState<EvaluationMaterialGenerationTarget[]>(
    [],
  );
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [result, setResult] =
    useState<EvaluationMaterialGenerationResult | null>(null);

  const preview = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await evaluationMaterialGenerationApi.preview({
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

  const create = async () => {
    const clientErrors = validateFilters(filters);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (
      !window.confirm("미리보기 조건으로 평가자료를 일괄 생성하시겠습니까?")
    ) {
      return;
    }
    try {
      setCreating(true);
      setError(null);
      setFieldErrors({});
      const response = await evaluationMaterialGenerationApi.create(filters);
      setResult(response.data ?? null);
      await preview();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setCreating(false);
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
        : "평가자료 생성을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVALUATION-MATERIAL-GENERATION"
        data-testid="evaluation-material-generation-page"
      >
        <PermissionState
          title="평가자료 생성 권한이 없습니다"
          message="R09 업무담당 권한 또는 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVALUATION-MATERIAL-GENERATION"
      data-testid="evaluation-material-generation-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 일괄처리 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              평가자료 생성
            </h1>
            <p className="mt-2 text-sm text-muted">
              인증 상태의 원천 실적만 평가자료로 일괄 생성하고 원천 실적과
              점수는 변경하지 않습니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={() => void preview()}
              data-testid="evaluation-material-generation-preview-button"
            >
              <Search size={16} /> 미리보기
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              onClick={() => void create()}
              disabled={creating}
              data-testid="evaluation-material-generation-create-button"
            >
              <FilePlus2 size={16} /> {creating ? "생성 중" : "생성 실행"}
            </button>
          </div>
        </div>
      </div>

      {result ? (
        <SuccessState
          title="평가자료 생성 요청 완료"
          message={`batchId ${result.batchId} / 대상 ${result.targetCount}건 / 생성 ${result.createdCount}건 / 제외 ${result.excludedCount}건`}
        />
      ) : null}
      {result ? (
        <a
          className="inline-flex rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
          href="/admin/evaluation-batch-results"
          data-testid="evaluation-material-generation-result-link"
        >
          처리 결과 조회로 이동
        </a>
      ) : null}
      {error ? <ErrorState title="평가자료 생성 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-6 shadow-md"
        data-testid="evaluation-material-generation-filter-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          <Field label="평가연도" error={fieldErrors.evaluationYear} required>
            <input
              id="evaluationYear"
              className="field-input"
              value={filters.evaluationYear}
              onChange={(event) =>
                setFilters({ ...filters, evaluationYear: event.target.value })
              }
              data-testid="evaluation-material-generation-year-input"
            />
          </Field>
          <Field label="평가영역">
            <input
              id="areaCode"
              className="field-input"
              value={filters.areaCode}
              onChange={(event) =>
                setFilters({ ...filters, areaCode: event.target.value })
              }
              placeholder="RESEARCH_CREATION"
              data-testid="evaluation-material-generation-area-input"
            />
          </Field>
          <Field label="조직">
            <input
              id="organizationCode"
              className="field-input"
              value={filters.organizationCode}
              onChange={(event) =>
                setFilters({ ...filters, organizationCode: event.target.value })
              }
              data-testid="evaluation-material-generation-organization-input"
            />
          </Field>
          <Field label="대상자">
            <input
              id="targetUserId"
              className="field-input"
              value={filters.targetUserId}
              onChange={(event) =>
                setFilters({ ...filters, targetUserId: event.target.value })
              }
              data-testid="evaluation-material-generation-target-input"
            />
          </Field>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            생성 대상 미리보기
          </h2>
          <button
            type="button"
            className="inline-flex items-center gap-2 text-sm font-semibold text-primary"
            onClick={() => void preview()}
            data-testid="evaluation-material-generation-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
        {loading ? (
          <LoadingState
            title="대상 조회 중"
            message="인증 원천 실적 후보를 조회합니다."
          />
        ) : null}
        {!loading && targets.length === 0 ? (
          <EmptyState
            title="생성 대상이 없습니다"
            message="평가연도·영역·조직·대상자 조건을 변경해 보세요."
          />
        ) : null}
        {!loading && targets.length > 0 ? (
          <div className="overflow-x-auto">
            <table
              className="min-w-full divide-y divide-ld text-left text-sm"
              data-testid="evaluation-material-generation-table"
            >
              <thead className="bg-lightsecondary text-xs uppercase text-muted">
                <tr>
                  <th className="px-4 py-3">원천ID</th>
                  <th className="px-4 py-3">평가연도</th>
                  <th className="px-4 py-3">영역</th>
                  <th className="px-4 py-3">조직</th>
                  <th className="px-4 py-3">대상자</th>
                  <th className="px-4 py-3">상태</th>
                  <th className="px-4 py-3">제목</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {targets.map((target) => (
                  <tr
                    key={`${target.sourceAchievementId}-${target.areaCode}`}
                    data-testid="evaluation-material-generation-row"
                  >
                    <td className="px-4 py-3 font-semibold text-primary">
                      {target.sourceAchievementId}
                    </td>
                    <td className="px-4 py-3">{target.evaluationYear}</td>
                    <td className="px-4 py-3">{target.areaCode}</td>
                    <td className="px-4 py-3">{target.organizationCode}</td>
                    <td className="px-4 py-3">{target.targetUserId}</td>
                    <td className="px-4 py-3">{target.sourceStatus}</td>
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
            onChange={(event) =>
              setPageSize(Number(event.target.value) as PageSize)
            }
            data-testid="evaluation-material-generation-size-select"
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
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]): Record<string, string> {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
