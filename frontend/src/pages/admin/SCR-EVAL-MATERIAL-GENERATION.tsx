import type React from "react";
import { RefreshCw, Search, Sparkles } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  evaluationMaterialGenerationApi,
  type ApiErrorField,
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

type GenerationFilters = {
  evaluationYear: string;
  areaCode: string;
  organizationCode: string;
  targetUserId: string;
  reason: string;
};

const initialFilters: GenerationFilters = {
  evaluationYear: "2026",
  areaCode: "RESEARCH_CREATION",
  organizationCode: "",
  targetUserId: "",
  reason: "인증 이상 원천 실적 평가자료 생성",
};

export function EvaluationMaterialGenerationPage() {
  const [filters, setFilters] = useState<GenerationFilters>(initialFilters);
  const [targets, setTargets] = useState<EvaluationMaterialGenerationTarget[]>(
    [],
  );
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
      `평가연도 ${filters.evaluationYear || "-"} / 영역 ${filters.areaCode || "-"} / 조직 ${filters.organizationCode || "전체"} / 대상자 ${filters.targetUserId || "전체"}`,
    [filters],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await evaluationMaterialGenerationApi.listEvaluationMaterialGenerations(
          {
            evaluationYear: filters.evaluationYear,
            areaCode: filters.areaCode,
            organizationCode: filters.organizationCode,
            targetUserId: parseOptionalNumber(filters.targetUserId),
            page,
            size: pageSize,
          },
        );
      setTargets(response.data?.targets ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
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
  };

  const generate = async () => {
    const clientErrors = validate(filters);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (
      !window.confirm(
        `${conditionSummary} 조건으로 평가자료를 생성하시겠습니까?`,
      )
    ) {
      return;
    }
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response =
        await evaluationMaterialGenerationApi.createEvaluationMaterialGeneration(
          {
            evaluationYear: filters.evaluationYear.trim(),
            areaCode: filters.areaCode.trim(),
            organizationCode: filters.organizationCode.trim() || undefined,
            targetUserId: parseOptionalNumber(filters.targetUserId),
            reason: filters.reason.trim(),
          },
        );
      const result = response.data;
      setLastBatchId(result?.generationBatchId ?? null);
      setSuccessMessage(
        result
          ? `평가자료 생성 완료: 생성배치ID ${result.generationBatchId}, 총 ${result.totalCount}건 / 성공 ${result.successCount}건 / 실패 ${result.failureCount}건 / 제외 ${result.excludedCount}건`
          : "평가자료 생성이 완료되었습니다.",
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
        : "평가자료 생성 처리를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVAL-MATERIAL-GENERATION"
        data-testid="evaluation-material-generation-page"
      >
        <PermissionState
          title="평가자료 생성 권한이 없습니다"
          message="R09 또는 평가자료 생성 실행 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVAL-MATERIAL-GENERATION"
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
              평가연도·영역·조직·대상자 조건을 확인한 뒤 인증 이상 원천 실적만
              평가자료로 일괄 생성합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="evaluation-material-generation-refresh-button"
          >
            <RefreshCw size={16} />
            새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="생성 후 같은 조건으로 대상과 생성배치ID를 재조회했습니다."
        />
      ) : null}
      {error ? <ErrorState title="평가자료 생성 오류" message={error} /> : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">생성 조건 설정</h2>
        <p className="mt-2 text-sm text-muted">
          실행 전 확인 조건: {conditionSummary}
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-5">
          <Field label="평가연도 *" error={fieldErrors.evaluationYear}>
            <input
              className="form-input"
              value={filters.evaluationYear}
              onChange={update("evaluationYear")}
              data-testid="evaluation-material-year-input"
            />
          </Field>
          <Field label="평가영역 *" error={fieldErrors.areaCode}>
            <input
              className="form-input"
              value={filters.areaCode}
              onChange={update("areaCode")}
              data-testid="evaluation-material-area-input"
            />
          </Field>
          <Field label="조직" error={fieldErrors.organizationCode}>
            <input
              className="form-input"
              value={filters.organizationCode}
              onChange={update("organizationCode")}
              data-testid="evaluation-material-organization-input"
            />
          </Field>
          <Field label="대상자 ID" error={fieldErrors.targetUserId}>
            <input
              className="form-input"
              value={filters.targetUserId}
              onChange={update("targetUserId")}
              data-testid="evaluation-material-target-user-input"
            />
          </Field>
          <Field label="생성 사유 *" error={fieldErrors.reason}>
            <input
              className="form-input"
              value={filters.reason}
              onChange={update("reason")}
              data-testid="evaluation-material-reason-input"
            />
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="evaluation-material-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={reset}
            data-testid="evaluation-material-reset-button"
          >
            조건 초기화
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-success px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void generate()}
            disabled={saving}
            data-testid="evaluation-material-generate-button"
          >
            <Sparkles size={16} />
            {saving ? "생성 중" : "평가자료 생성"}
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-semibold text-dark">생성 대상 및 결과</h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="form-select ml-2"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as PageSize)
              }
              data-testid="evaluation-material-page-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {lastBatchId ? (
          <p className="mt-2 text-sm text-success">
            최근 생성배치ID: {lastBatchId}
          </p>
        ) : null}
        {loading ? (
          <LoadingState title="평가자료 생성 대상을 불러오는 중입니다" />
        ) : null}
        {!loading && targets.length === 0 ? (
          <EmptyState title="조회된 평가자료 생성 대상이 없습니다" />
        ) : null}
        {!loading && targets.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table
              className="min-w-full divide-y divide-ld text-sm"
              data-testid="evaluation-material-target-table"
            >
              <thead className="bg-lightgray text-left text-muted">
                <tr>
                  <th className="px-4 py-3">원천 업적</th>
                  <th className="px-4 py-3">평가연도</th>
                  <th className="px-4 py-3">영역</th>
                  <th className="px-4 py-3">조직</th>
                  <th className="px-4 py-3">대상자</th>
                  <th className="px-4 py-3">원천상태</th>
                  <th className="px-4 py-3">생성상태</th>
                  <th className="px-4 py-3">생성배치ID</th>
                  <th className="px-4 py-3">제외 사유</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {targets.map((target) => (
                  <tr
                    key={`${target.sourceAchievementId}-${target.generationBatchId ?? "candidate"}`}
                    data-testid="evaluation-material-target-row"
                  >
                    <td className="px-4 py-3">{target.sourceAchievementId}</td>
                    <td className="px-4 py-3">{target.evaluationYear}</td>
                    <td className="px-4 py-3">{target.areaCode}</td>
                    <td className="px-4 py-3">{target.organizationCode}</td>
                    <td className="px-4 py-3">{target.targetUserId}</td>
                    <td className="px-4 py-3">
                      {sourceStatusLabel(target.sourceStatus)}
                    </td>
                    <td className="px-4 py-3">
                      {generationStatusLabel(target.generationStatus)}
                    </td>
                    <td className="px-4 py-3">
                      {target.generationBatchId ?? "-"}
                    </td>
                    <td className="px-4 py-3">
                      {target.excludedReason ?? "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <div className="mt-4 flex items-center justify-between text-sm text-muted">
          <span>총 {totalElements}건</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => Math.max(value - 1, 0))}
            data-testid="evaluation-material-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => value + 1)}
            data-testid="evaluation-material-next-page-button"
          >
            다음
          </button>
        </div>
      </section>
    </section>
  );

  function update(field: keyof GenerationFilters) {
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

function validate(filters: GenerationFilters) {
  const errors: Record<string, string> = {};
  if (!filters.evaluationYear.trim())
    errors.evaluationYear = "평가연도를 입력하세요.";
  if (!filters.areaCode.trim()) errors.areaCode = "평가영역을 입력하세요.";
  if (!filters.reason.trim()) errors.reason = "생성 사유를 입력하세요.";
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

function sourceStatusLabel(status: string) {
  return status === "CERTIFIED"
    ? "인증"
    : status === "EVALUATION_CONFIRMED"
      ? "평가확정"
      : status;
}

function generationStatusLabel(status: string) {
  if (status === "READY") return "생성 가능";
  if (status === "GENERATED") return "생성됨";
  if (status === "EXCLUDED") return "제외";
  return status;
}
