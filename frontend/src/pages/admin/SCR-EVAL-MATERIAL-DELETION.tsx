import type React from "react";
import { RefreshCw, Search, Trash2 } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  evaluationMaterialDeletionApi,
  type ApiErrorField,
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

type DeletionFilters = {
  evaluationYear: string;
  areaCode: string;
  generationBatchId: string;
  deletionReason: string;
};

const initialFilters: DeletionFilters = {
  evaluationYear: "",
  areaCode: "",
  generationBatchId: "",
  deletionReason: "",
};

export function EvaluationMaterialDeletionPage() {
  const [filters, setFilters] = useState<DeletionFilters>(initialFilters);
  const [targets, setTargets] = useState<EvaluationMaterialDeletionTarget[]>(
    [],
  );
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [deletableCount, setDeletableCount] = useState(0);
  const [previewToken, setPreviewToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const conditionSummary = useMemo(
    () =>
      `평가연도 ${filters.evaluationYear || "-"} / 영역 ${filters.areaCode || "-"} / 생성배치ID ${filters.generationBatchId || "-"}`,
    [filters],
  );

  const loadPreview = async () => {
    const clientErrors = validatePreview(filters);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response =
        await evaluationMaterialDeletionApi.previewEvaluationMaterialDeletion({
          evaluationYear: filters.evaluationYear,
          areaCode: filters.areaCode,
          generationBatchId: filters.generationBatchId,
          page,
          size: pageSize,
        });
      setTargets(response.data?.targets ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setDeletableCount(response.data?.deletableCount ?? 0);
      setPreviewToken(response.data?.previewToken ?? null);
      setFieldErrors({});
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadPreview();
  }, [page, pageSize]);

  const preview = async () => {
    setPage(0);
    await loadPreview();
  };

  const reset = () => {
    setFilters(initialFilters);
    setTargets([]);
    setPreviewToken(null);
    setFieldErrors({});
    setError(null);
    setSuccessMessage(null);
  };

  const deleteMaterials = async () => {
    const clientErrors = validateDelete(filters, previewToken);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (
      !window.confirm(
        `${conditionSummary} 미리보기 대상 ${deletableCount}건을 삭제하시겠습니까?`,
      )
    ) {
      return;
    }
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response =
        await evaluationMaterialDeletionApi.createEvaluationMaterialDeletion({
          evaluationYear: filters.evaluationYear.trim(),
          areaCode: filters.areaCode.trim(),
          generationBatchId: filters.generationBatchId.trim(),
          deletionReason: filters.deletionReason.trim(),
          previewToken: previewToken ?? "",
        });
      const result = response.data;
      setSuccessMessage(
        result
          ? `평가자료 삭제 완료: 삭제배치ID ${result.deletionBatchId}, 총 ${result.totalCount}건 / 성공 ${result.successCount}건 / 실패 ${result.failureCount}건 / 제외 ${result.excludedCount}건`
          : "평가자료 삭제가 완료되었습니다.",
      );
      await loadPreview();
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
        : "평가자료 삭제 처리를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVAL-MATERIAL-DELETION"
        data-testid="evaluation-material-deletion-page"
      >
        <PermissionState
          title="평가자료 삭제 권한이 없습니다"
          message="R09 또는 평가자료 삭제 실행 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVAL-MATERIAL-DELETION"
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
              생성배치 기준 삭제대상을 미리 확인하고 삭제사유를 남긴 뒤
              평가자료만 논리삭제합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void loadPreview()}
            data-testid="evaluation-material-deletion-refresh-button"
          >
            <RefreshCw size={16} />
            새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="삭제 후 같은 조건으로 대상과 삭제가능 건수를 재조회했습니다."
        />
      ) : null}
      {error ? <ErrorState title="평가자료 삭제 오류" message={error} /> : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">삭제 조건 및 사유</h2>
        <p className="mt-2 text-sm text-muted">
          실행 전 확인 조건: {conditionSummary}
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-4">
          <Field label="평가연도 *" error={fieldErrors.evaluationYear}>
            <input
              className="form-input"
              value={filters.evaluationYear}
              onChange={update("evaluationYear")}
              data-testid="evaluation-material-deletion-year-input"
            />
          </Field>
          <Field label="평가영역 *" error={fieldErrors.areaCode}>
            <input
              className="form-input"
              value={filters.areaCode}
              onChange={update("areaCode")}
              data-testid="evaluation-material-deletion-area-input"
            />
          </Field>
          <Field label="생성배치ID *" error={fieldErrors.generationBatchId}>
            <input
              className="form-input"
              value={filters.generationBatchId}
              onChange={update("generationBatchId")}
              data-testid="evaluation-material-deletion-batch-input"
            />
          </Field>
          <Field label="삭제사유 *" error={fieldErrors.deletionReason}>
            <input
              className="form-input"
              value={filters.deletionReason}
              onChange={update("deletionReason")}
              data-testid="evaluation-material-deletion-reason-input"
            />
          </Field>
        </div>
        {fieldErrors.previewToken ? (
          <p className="mt-2 text-xs text-error">{fieldErrors.previewToken}</p>
        ) : null}
        <p className="mt-3 text-sm text-muted">
          안내: 원천 실적과 수동 등록 자료는 삭제하지 않습니다. 평가확정 자료는
          확정취소 후 처리합니다.
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void preview()}
            data-testid="evaluation-material-deletion-preview-button"
          >
            <Search size={16} />
            미리보기
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={reset}
            data-testid="evaluation-material-deletion-reset-button"
          >
            취소
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-error px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void deleteMaterials()}
            disabled={saving || !previewToken}
            data-testid="evaluation-material-deletion-delete-button"
          >
            <Trash2 size={16} />
            {saving ? "삭제 중" : "삭제 실행"}
          </button>
        </div>
        {!previewToken ? (
          <p className="mt-2 text-xs text-muted">
            삭제 전 미리보기를 먼저 실행하세요.
          </p>
        ) : null}
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              삭제대상 미리보기
            </h2>
            <p className="mt-1 text-sm text-success">
              삭제가능 {deletableCount}건 / 전체 {totalElements}건
            </p>
            {previewToken ? (
              <p className="mt-1 text-xs text-muted">
                previewToken: {previewToken}
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
              data-testid="evaluation-material-deletion-page-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="평가자료 삭제 대상을 불러오는 중입니다" />
        ) : null}
        {!loading && targets.length === 0 ? (
          <EmptyState title="조회된 평가자료 삭제 대상이 없습니다" />
        ) : null}
        {!loading && targets.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table
              className="min-w-full divide-y divide-ld text-sm"
              data-testid="evaluation-material-deletion-target-table"
            >
              <thead className="bg-lightgray text-left text-muted">
                <tr>
                  <th className="px-4 py-3">평가자료ID</th>
                  <th className="px-4 py-3">대상자</th>
                  <th className="px-4 py-3">평가연도</th>
                  <th className="px-4 py-3">영역</th>
                  <th className="px-4 py-3">생성배치ID</th>
                  <th className="px-4 py-3">확정상태</th>
                  <th className="px-4 py-3">삭제가능여부</th>
                  <th className="px-4 py-3">제외 사유</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {targets.map((target) => (
                  <tr
                    key={target.evaluationMaterialId}
                    data-testid="evaluation-material-deletion-target-row"
                  >
                    <td className="px-4 py-3">{target.evaluationMaterialId}</td>
                    <td className="px-4 py-3">{target.targetUserId}</td>
                    <td className="px-4 py-3">{target.evaluationYear}</td>
                    <td className="px-4 py-3">{target.areaCode}</td>
                    <td className="px-4 py-3">{target.generationBatchId}</td>
                    <td className="px-4 py-3">
                      {finalStatusLabel(target.finalStatus)}
                    </td>
                    <td className="px-4 py-3">
                      {target.canDelete ? "삭제 가능" : "삭제 불가"}
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
            data-testid="evaluation-material-deletion-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => value + 1)}
            data-testid="evaluation-material-deletion-next-page-button"
          >
            다음
          </button>
        </div>
      </section>
    </section>
  );

  function update(field: keyof DeletionFilters) {
    return (event: React.ChangeEvent<HTMLInputElement>) => {
      setFilters((current) => ({ ...current, [field]: event.target.value }));
      setFieldErrors({});
      setSuccessMessage(null);
      setPreviewToken(null);
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

function validatePreview(filters: DeletionFilters) {
  const errors: Record<string, string> = {};
  if (!filters.evaluationYear.trim())
    errors.evaluationYear = "평가연도를 입력하세요.";
  if (!filters.areaCode.trim()) errors.areaCode = "평가영역을 입력하세요.";
  if (!filters.generationBatchId.trim())
    errors.generationBatchId = "생성배치ID를 입력하세요.";
  return errors;
}

function validateDelete(filters: DeletionFilters, previewToken: string | null) {
  const errors = validatePreview(filters);
  if (!filters.deletionReason.trim())
    errors.deletionReason = "삭제사유를 입력하세요.";
  if (!previewToken)
    errors.previewToken = "삭제 전 미리보기를 먼저 실행하세요.";
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function finalStatusLabel(status: string) {
  if (status === "CERTIFIED") return "인증";
  if (status === "EVALUATION_CONFIRMED") return "평가확정";
  if (status === "DELETED") return "삭제";
  return status;
}
