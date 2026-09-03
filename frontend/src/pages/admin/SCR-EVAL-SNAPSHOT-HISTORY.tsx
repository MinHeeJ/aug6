import type React from "react";
import { Download, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  evaluationSnapshotApi,
  type ApiErrorField,
  type EvaluationSnapshotDetail,
  type EvaluationSnapshotRow,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type SnapshotFilters = {
  evaluationYear: string;
  finalizationPoint: string;
};

const initialFilters: SnapshotFilters = {
  evaluationYear: "2026",
  finalizationPoint: "",
};

export function EvaluationSnapshotHistoryPage() {
  const [filters, setFilters] = useState<SnapshotFilters>(initialFilters);
  const [rows, setRows] = useState<EvaluationSnapshotRow[]>([]);
  const [selectedSnapshotId, setSelectedSnapshotId] = useState<string | null>(
    null,
  );
  const [detail, setDetail] = useState<EvaluationSnapshotDetail | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const selectedRow = useMemo(
    () =>
      rows.find((row) => row.snapshotId === selectedSnapshotId) ??
      rows[0] ??
      null,
    [rows, selectedSnapshotId],
  );

  const conditionSummary = useMemo(
    () =>
      `평가연도 ${filters.evaluationYear || "전체"} / 확정시점 ${filters.finalizationPoint || "전체"}`,
    [filters],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await evaluationSnapshotApi.listEvaluationSnapshots({
        evaluationYear: filters.evaluationYear,
        finalizationPoint: filters.finalizationPoint,
        page,
        size: pageSize,
      });
      const nextRows = response.data?.results ?? [];
      setRows(nextRows);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelectedSnapshotId(
        (current) => current ?? nextRows[0]?.snapshotId ?? null,
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

  useEffect(() => {
    if (!selectedRow?.snapshotId) {
      setDetail(null);
      return;
    }
    void loadDetail(selectedRow.snapshotId);
  }, [selectedRow?.snapshotId]);

  const search = async () => {
    setPage(0);
    setSelectedSnapshotId(null);
    setDetail(null);
    await load();
  };

  const loadDetail = async (snapshotId: string) => {
    try {
      setDetailLoading(true);
      setError(null);
      const response =
        await evaluationSnapshotApi.getEvaluationSnapshotDetail(snapshotId);
      setDetail(response.data ?? null);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setDetailLoading(false);
    }
  };

  const download = async () => {
    try {
      setSuccessMessage(null);
      const response =
        await evaluationSnapshotApi.downloadEvaluationSnapshotsExcel({
          evaluationYear: filters.evaluationYear,
          finalizationPoint: filters.finalizationPoint,
        });
      const data = response.data;
      setSuccessMessage(
        data
          ? `${data.fileName} 다운로드 요청이 기록되었습니다. 대상 ${data.rowCount}건.`
          : "Excel 다운로드 요청이 기록되었습니다.",
      );
    } catch (caught) {
      handleApiError(caught);
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
        : "시점 데이터 조회를 완료하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-EVAL-SNAPSHOT-HISTORY"
        data-testid="evaluation-snapshot-page"
      >
        <PermissionState
          title="시점 데이터 관리 권한이 없습니다"
          message="R04, R08, R09 또는 시점 데이터 조회 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVAL-SNAPSHOT-HISTORY"
      data-testid="evaluation-snapshot-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">파일·데이터 관리 / 데이터 이력 관리</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          시점 데이터 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          평가확정 시점의 기준정보·평가자료 snapshot과 보존 결과를 조회합니다.
        </p>
      </div>

      {error ? (
        <ErrorState title="시점 데이터 조회 오류" message={error} />
      ) : null}
      {successMessage ? (
        <SuccessState title="Excel 다운로드" message={successMessage} />
      ) : null}

      <section className="rounded-md bg-white p-6 shadow-md">
        <h2 className="text-lg font-semibold text-dark">검색조건</h2>
        <p className="mt-2 text-sm text-muted">검색 조건: {conditionSummary}</p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field label="평가연도" error={fieldErrors.evaluationYear}>
            <input
              className="form-input"
              value={filters.evaluationYear}
              onChange={update("evaluationYear")}
              data-testid="evaluation-snapshot-year-input"
            />
          </Field>
          <Field label="확정시점" error={fieldErrors.finalizationPoint}>
            <input
              className="form-input"
              value={filters.finalizationPoint}
              onChange={update("finalizationPoint")}
              data-testid="evaluation-snapshot-point-input"
            />
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => void search()}
            data-testid="evaluation-snapshot-search-button"
          >
            <Search size={16} />
            조회
          </button>
          <button
            type="button"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void download()}
            data-testid="evaluation-snapshot-excel-button"
          >
            <Download size={16} />
            Excel
          </button>
        </div>
      </section>

      <section className="rounded-md bg-white p-6 shadow-md">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-dark">확정시점 목록</h2>
            <p className="mt-1 text-sm text-muted">
              현재 기준정보·평가자료 변경 기능은 제공하지 않습니다.
            </p>
          </div>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="form-select ml-2"
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as PageSize)
              }
              data-testid="evaluation-snapshot-page-size-select"
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? (
          <LoadingState title="시점 데이터를 불러오는 중입니다" />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 시점 데이터가 없습니다" />
        ) : null}
        <div className="mt-4 overflow-x-auto">
          <table
            className="min-w-full divide-y divide-ld text-sm"
            data-testid="evaluation-snapshot-table"
          >
            <thead className="bg-lightgray text-left text-muted">
              <tr>
                <th className="px-4 py-3">선택</th>
                <th className="px-4 py-3">확정시점</th>
                <th className="px-4 py-3">기준정보 snapshot</th>
                <th className="px-4 py-3">평가자료 snapshot</th>
                <th className="px-4 py-3">보존 결과</th>
                <th className="px-4 py-3">상태</th>
                <th className="px-4 py-3">요청ID</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ld">
              {rows.map((row) => (
                <tr key={row.snapshotId} data-testid="evaluation-snapshot-row">
                  <td className="px-4 py-3">
                    <input
                      type="radio"
                      name="evaluation-snapshot"
                      checked={
                        (selectedSnapshotId ?? rows[0]?.snapshotId) ===
                        row.snapshotId
                      }
                      onChange={() => setSelectedSnapshotId(row.snapshotId)}
                      data-testid="evaluation-snapshot-radio"
                    />
                  </td>
                  <td className="px-4 py-3 font-medium text-dark">
                    {row.finalizationPoint}
                  </td>
                  <td className="px-4 py-3">{row.ruleSnapshotRef}</td>
                  <td className="px-4 py-3">{row.materialSnapshotRef}</td>
                  <td className="px-4 py-3">{row.preservedResultRef}</td>
                  <td className="px-4 py-3">
                    {snapshotStatusLabel(row.snapshotStatus)}
                  </td>
                  <td className="px-4 py-3">{row.requestId}</td>
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
            data-testid="evaluation-snapshot-prev-page-button"
          >
            이전
          </button>
          <span>{page + 1} 페이지</span>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2"
            onClick={() => setPage((value) => value + 1)}
            data-testid="evaluation-snapshot-next-page-button"
          >
            다음
          </button>
        </div>
      </section>

      <section
        className="rounded-md bg-white p-6 shadow-md"
        data-testid="evaluation-snapshot-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">선택 시점 상세</h2>
        <p className="mt-2 text-sm text-muted">
          선택 snapshot: {selectedRow?.snapshotId ?? "미선택"}
        </p>
        <div className="mt-4 flex flex-wrap gap-2 text-xs font-semibold text-muted">
          <span className="rounded-full bg-lightgray px-3 py-1">
            기준정보 snapshot
          </span>
          <span className="rounded-full bg-lightgray px-3 py-1">
            평가자료 snapshot
          </span>
          <span className="rounded-full bg-lightgray px-3 py-1">
            보존 결과 대조
          </span>
        </div>
        {detailLoading ? (
          <LoadingState title="시점 상세를 불러오는 중입니다" />
        ) : null}
        {!detailLoading && !selectedRow ? (
          <EmptyState title="상세를 볼 시점 데이터를 선택하세요" />
        ) : null}
        {!detailLoading && detail ? (
          <div className="mt-4 grid gap-4 lg:grid-cols-3">
            <SnapshotCard
              title="기준정보 snapshot"
              value={detail.ruleSnapshotJson}
              testId="evaluation-snapshot-rule-card"
            />
            <SnapshotCard
              title="평가자료 snapshot"
              value={detail.materialSnapshotJson}
              testId="evaluation-snapshot-material-card"
            />
            <SnapshotCard
              title="보존 결과 대조"
              value={detail.preservedResultJson}
              testId="evaluation-snapshot-result-card"
            />
            <p className="text-sm text-muted lg:col-span-3">
              {detail.readOnlyNotice}
            </p>
          </div>
        ) : null}
      </section>
    </section>
  );

  function update(key: keyof SnapshotFilters) {
    return (event: React.ChangeEvent<HTMLInputElement>) =>
      setFilters((current) => ({ ...current, [key]: event.target.value }));
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
    <label className="text-sm font-medium text-dark">
      {label}
      <div className="mt-1">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function SnapshotCard({
  title,
  value,
  testId,
}: {
  title: string;
  value: string;
  testId: string;
}) {
  return (
    <article
      className="rounded-md border border-ld bg-lightgray p-4"
      data-testid={testId}
    >
      <h3 className="text-sm font-semibold text-dark">{title}</h3>
      <pre className="mt-2 whitespace-pre-wrap break-all text-xs text-muted">
        {value}
      </pre>
    </article>
  );
}

function snapshotStatusLabel(status: string) {
  if (status === "PRESERVED") return "보존";
  if (status === "CANCELLED") return "확정취소";
  if (status === "RECONFIRMED") return "재확정";
  return status;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
