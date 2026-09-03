import type React from "react";
import { RefreshCw, Search, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  evaluationMaterialDeletionApi,
  type ApiErrorField,
  type EvaluationMaterialDeletionResult,
  type EvaluationMaterialDeletionTarget,
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
  generationBatchId: string;
  deleteReason: string;
};

const initialFilters: Filters = {
  evaluationYear: "2026",
  areaCode: "",
  generationBatchId: "",
  deleteReason: "",
};

export function EvaluationMaterialDeletionPage() {
  const [filters, setFilters] = useState<Filters>(initialFilters);
  const [targets, setTargets] = useState<EvaluationMaterialDeletionTarget[]>(
    [],
  );
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [result, setResult] = useState<EvaluationMaterialDeletionResult | null>(
    null,
  );

  const preview = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await evaluationMaterialDeletionApi.preview({
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

  const deleteMaterials = async () => {
    const clientErrors = validateFilters(filters);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (
      !window.confirm(
        "미리보기 조건의 일괄 생성 평가자료를 논리 삭제하시겠습니까?",
      )
    ) {
      return;
    }
    try {
      setDeleting(true);
      setError(null);
      setFieldErrors({});
      const response = await evaluationMaterialDeletionApi.delete(filters);
      setResult(response.data ?? null);
      await preview();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setDeleting(false);
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
        : "평가자료 삭제를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVALUATION-MATERIAL-DELETION"
        data-testid="evaluation-material-deletion-page"
      >
        <PermissionState
          title="평가자료 삭제 권한이 없습니다"
          message="R09 업무담당 권한 또는 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVALUATION-MATERIAL-DELETION"
      data-testid="evaluation-material-deletion-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 일괄처리 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              평가자료 삭제
            </h1>
            <p className="mt-2 text-sm text-muted">
              생성배치 기준으로 일괄 생성 평가자료만 논리 삭제하고 원천 실적과
              수동 등록 자료는 보존합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
              onClick={() => void preview()}
              data-testid="evaluation-material-deletion-preview-button"
            >
              <Search size={16} /> 삭제대상 미리보기
            </button>
            <button
              type="button"
              className="inline-flex h-10 items-center gap-2 rounded-md bg-error px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              onClick={() => void deleteMaterials()}
              disabled={deleting}
              data-testid="evaluation-material-deletion-delete-button"
            >
              <Trash2 size={16} /> {deleting ? "삭제 중" : "삭제 실행"}
            </button>
          </div>
        </div>
      </div>

      {result ? (
        <SuccessState
          title="평가자료 삭제 요청 완료"
          message={`batchId ${result.batchId} / 대상 ${result.targetCount}건 / 삭제 ${result.deletedCount}건 / 제외 ${result.excludedCount}건`}
        />
      ) : null}
      {result ? (
        <a
          className="inline-flex rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
          href="/admin/evaluation-batch-results"
          data-testid="evaluation-material-deletion-result-link"
        >
          처리 결과 조회로 이동
        </a>
      ) : null}
      {error ? <ErrorState title="평가자료 삭제 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-6 shadow-md"
        data-testid="evaluation-material-deletion-filter-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          <Field label="평가연도" error={fieldErrors.evaluationYear} required>
            <input
              id="deletionEvaluationYear"
              className="field-input"
              value={filters.evaluationYear}
              onChange={(event) =>
                setFilters({ ...filters, evaluationYear: event.target.value })
              }
              data-testid="evaluation-material-deletion-year-input"
            />
          </Field>
          <Field label="평가영역">
            <input
              id="deletionAreaCode"
              className="field-input"
              value={filters.areaCode}
              onChange={(event) =>
                setFilters({ ...filters, areaCode: event.target.value })
              }
              placeholder="EDUCATION"
              data-testid="evaluation-material-deletion-area-input"
            />
          </Field>
          <Field
            label="생성배치"
            error={fieldErrors.generationBatchId}
            required
          >
            <input
              id="deletionGenerationBatchId"
              className="field-input"
              value={filters.generationBatchId}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  generationBatchId: event.target.value,
                })
              }
              data-testid="evaluation-material-deletion-generation-batch-input"
            />
          </Field>
          <Field label="삭제사유" error={fieldErrors.deleteReason} required>
            <input
              id="deleteReason"
              className="field-input"
              value={filters.deleteReason}
              onChange={(event) =>
                setFilters({ ...filters, deleteReason: event.target.value })
              }
              data-testid="evaluation-material-deletion-reason-input"
            />
          </Field>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-md">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">삭제대상 미리보기</h2>
          <button
            type="button"
            className="inline-flex items-center gap-2 text-sm font-semibold text-primary"
            onClick={() => void preview()}
            data-testid="evaluation-material-deletion-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
        {loading ? (
          <LoadingState
            title="삭제대상 조회 중"
            message="생성배치 기준 평가자료를 조회합니다."
          />
        ) : null}
        {!loading && targets.length === 0 ? (
          <EmptyState
            title="삭제 대상이 없습니다"
            message="평가연도·영역·생성배치 조건을 변경해 보세요."
          />
        ) : null}
        {!loading && targets.length > 0 ? (
          <div className="overflow-x-auto">
            <table
              className="min-w-full divide-y divide-ld text-left text-sm"
              data-testid="evaluation-material-deletion-table"
            >
              <thead className="bg-lightsecondary text-xs uppercase text-muted">
                <tr>
                  <th className="px-4 py-3">자료ID</th>
                  <th className="px-4 py-3">평가연도</th>
                  <th className="px-4 py-3">영역</th>
                  <th className="px-4 py-3">대상자</th>
                  <th className="px-4 py-3">생성배치</th>
                  <th className="px-4 py-3">상태</th>
                  <th className="px-4 py-3">제목</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {targets.map((target) => (
                  <tr
                    key={target.evaluationMaterialId}
                    data-testid="evaluation-material-deletion-row"
                  >
                    <td className="px-4 py-3 font-semibold text-primary">
                      {target.evaluationMaterialId}
                    </td>
                    <td className="px-4 py-3">{target.evaluationYear}</td>
                    <td className="px-4 py-3">{target.areaCode}</td>
                    <td className="px-4 py-3">{target.targetUserId}</td>
                    <td className="px-4 py-3">{target.generationBatchId}</td>
                    <td className="px-4 py-3">{target.materialStatus}</td>
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
            data-testid="evaluation-material-deletion-size-select"
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
  if (!filters.generationBatchId.trim())
    errors.generationBatchId = "생성배치ID를 입력하세요.";
  if (!filters.deleteReason.trim())
    errors.deleteReason = "삭제사유를 입력하세요.";
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]): Record<string, string> {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
