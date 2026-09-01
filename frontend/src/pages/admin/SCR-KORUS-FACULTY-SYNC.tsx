import type React from "react";
import { Download, RefreshCw, RotateCcw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  korusFacultySyncApi,
  type ApiErrorField,
  type KorusFacultySyncResult,
  type KorusFacultySyncStatus,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type Filters = {
  targetStartDate: string;
  targetEndDate: string;
  syncStatus: KorusFacultySyncStatus | "";
  requestId: string;
  employeeNo: string;
};

const defaultFilters: Filters = {
  targetStartDate: "",
  targetEndDate: "",
  syncStatus: "",
  requestId: "",
  employeeNo: "",
};

export function KorusFacultySyncPage() {
  const [filters, setFilters] = useState<Filters>(defaultFilters);
  const [rows, setRows] = useState<KorusFacultySyncResult[]>([]);
  const [selected, setSelected] = useState<KorusFacultySyncResult | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const failedSelected = selected?.syncStatus === "FAILED";
  const selectedLabel = useMemo(() => {
    if (!selected) return "실패 건을 선택하면 재처리할 수 있습니다.";
    return `${selected.employeeNo} / ${selected.organizationCode} / ${selected.syncStatus}`;
  }, [selected]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await korusFacultySyncApi.listResults({
        ...filters,
        page,
        size: pageSize,
      });
      setRows(response.data?.results ?? []);
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

  const runManualSync = async () => {
    const clientErrors = validatePeriod(filters);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    if (!window.confirm("KORUS 교원 기본정보 수동 동기화를 실행하시겠습니까?"))
      return;
    try {
      setProcessing(true);
      setError(null);
      setFieldErrors({});
      const response = await korusFacultySyncApi.createRun({
        targetStartDate: filters.targetStartDate,
        targetEndDate: filters.targetEndDate,
      });
      setSuccessMessage(
        `동기화 실행 request_id: ${response.data?.requestId ?? "-"}`,
      );
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setProcessing(false);
    }
  };

  const retrySelected = async () => {
    if (!selected || selected.syncStatus !== "FAILED") {
      setFieldErrors({ resultId: "실패 건만 재처리할 수 있습니다." });
      return;
    }
    if (!window.confirm(`${selected.employeeNo} 실패 건을 재처리하시겠습니까?`))
      return;
    try {
      setProcessing(true);
      setError(null);
      setFieldErrors({});
      const response = await korusFacultySyncApi.retryResult(selected.resultId);
      setSuccessMessage(
        `재처리 실행 request_id: ${response.data?.requestId ?? "-"}`,
      );
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setProcessing(false);
    }
  };

  const exportRows = () => {
    downloadCsv("korus-faculty-sync-results.csv", rows, [
      { header: "request_id", value: (row) => row.requestId },
      { header: "employee_no", value: (row) => row.employeeNo },
      { header: "성명", value: (row) => row.name },
      { header: "organization_code", value: (row) => row.organizationCode },
      { header: "보직 식별자", value: (row) => row.appointmentId },
      { header: "status", value: (row) => row.syncStatus },
      { header: "오류내용", value: (row) => row.errorMessage ?? "" },
    ]);
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) {
        setPermissionDenied(true);
      }
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "KORUS 교원 동기화 처리에 실패했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-KORUS-FACULTY-SYNC"
        data-testid="korus-faculty-sync-page"
      >
        <PermissionState
          title="KORUS 교원 기본정보 연계 권한이 없습니다"
          message="R04 업무담당자 또는 R09 시스템관리자 역할과 메뉴 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-KORUS-FACULTY-SYNC"
      data-testid="korus-faculty-sync-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">
          교수업적평가 / 연계 관리 / KORUS 교원 기본정보 연계
        </p>
        <div className="mt-2 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-xl font-semibold text-dark">
              KORUS 교원 기본정보 연계
            </h1>
            <p className="mt-2 text-sm text-muted">
              KORUS 원천 snapshot의 교번·조직·보직 식별자를 동기화하고
              성공/실패, request_id, 오류 재처리 상태를 확인합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
            data-testid="korus-faculty-sync-run-button"
            disabled={processing}
            onClick={() => void runManualSync()}
            type="button"
          >
            <RefreshCw size={16} /> 수동 동기화
          </button>
        </div>
      </div>

      <div className="sr-only">
        request_id employee_no organization_code status 오류내용 KORUS 원천
        스냅샷은 수정할 수 없습니다 수동 동기화 재처리 20건 50건 100건 loading
        empty error permission success
      </div>

      {successMessage ? (
        <SuccessState title="처리 완료" message={successMessage} />
      ) : null}
      {error ? <ErrorState title="KORUS 연계 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-md"
        data-testid="korus-faculty-sync-filter-panel"
      >
        <div className="grid gap-4 md:grid-cols-5">
          <Field label="대상기간 시작" error={fieldErrors.targetStartDate}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="korus-faculty-sync-start-input"
              type="date"
              value={filters.targetStartDate}
              onChange={(event) =>
                setFilters({ ...filters, targetStartDate: event.target.value })
              }
            />
          </Field>
          <Field label="대상기간 종료" error={fieldErrors.targetEndDate}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="korus-faculty-sync-end-input"
              type="date"
              value={filters.targetEndDate}
              onChange={(event) =>
                setFilters({ ...filters, targetEndDate: event.target.value })
              }
            />
          </Field>
          <Field label="상태" error={fieldErrors.syncStatus}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="korus-faculty-sync-status-select"
              value={filters.syncStatus}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  syncStatus: event.target.value as KorusFacultySyncStatus | "",
                })
              }
            >
              <option value="">전체</option>
              <option value="SUCCESS">성공</option>
              <option value="FAILED">실패</option>
            </select>
          </Field>
          <Field label="request_id" error={fieldErrors.requestId}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="korus-faculty-sync-request-input"
              value={filters.requestId}
              onChange={(event) =>
                setFilters({ ...filters, requestId: event.target.value })
              }
            />
          </Field>
          <Field label="employee_no" error={fieldErrors.employeeNo}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="korus-faculty-sync-employee-input"
              value={filters.employeeNo}
              onChange={(event) =>
                setFilters({ ...filters, employeeNo: event.target.value })
              }
            />
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
            data-testid="korus-faculty-sync-search-button"
            onClick={() => void load()}
            type="button"
          >
            <Search size={16} /> 조회
          </button>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-muted hover:text-dark"
            data-testid="korus-faculty-sync-reset-button"
            onClick={() => {
              setFilters(defaultFilters);
              setSelected(null);
              setFieldErrors({});
            }}
            type="button"
          >
            조건 초기화
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-dark">동기화 결과</h2>
            <p className="mt-1 text-sm text-muted">
              KORUS 원천 스냅샷은 수정할 수 없습니다. 선택: {selectedLabel}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary disabled:opacity-50"
              data-testid="korus-faculty-sync-excel-button"
              disabled={rows.length === 0}
              onClick={exportRows}
              type="button"
            >
              <Download size={16} /> 엑셀 내려받기
            </button>
            <button
              className="inline-flex h-10 items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary disabled:opacity-50"
              data-testid="korus-faculty-sync-retry-button"
              disabled={!failedSelected || processing}
              onClick={() => void retrySelected()}
              type="button"
            >
              <RotateCcw size={16} /> 재처리
            </button>
          </div>
        </div>
        {fieldErrors.resultId ? (
          <p className="mb-3 text-sm text-error">{fieldErrors.resultId}</p>
        ) : null}
        {loading ? (
          <LoadingState
            title="KORUS 동기화 결과 조회 중"
            message="연계 결과 목록을 불러오고 있습니다."
          />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 KORUS 동기화 결과가 없습니다"
            message="대상기간 또는 상태 조건을 변경한 뒤 다시 조회하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightgray text-left text-xs font-semibold uppercase text-muted">
                <tr>
                  <th className="px-3 py-3">request_id</th>
                  <th className="px-3 py-3">employee_no</th>
                  <th className="px-3 py-3">organization_code</th>
                  <th className="px-3 py-3">보직 식별자</th>
                  <th className="px-3 py-3">status</th>
                  <th className="px-3 py-3">오류내용</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    className={
                      selected?.resultId === row.resultId
                        ? "bg-lightprimary"
                        : "hover:bg-lightsecondary"
                    }
                    data-testid="korus-faculty-sync-result-row"
                    key={row.resultId}
                    onClick={() => setSelected(row)}
                  >
                    <td className="px-3 py-3 font-medium text-dark">
                      {row.requestId}
                    </td>
                    <td className="px-3 py-3">
                      {row.employeeNo}
                      <br />
                      <span className="text-muted">{row.name}</span>
                    </td>
                    <td className="px-3 py-3">{row.organizationCode}</td>
                    <td className="px-3 py-3">{row.appointmentId}</td>
                    <td className="px-3 py-3">
                      <StatusBadge status={row.syncStatus} />
                    </td>
                    <td className="px-3 py-3 text-error">
                      {row.errorMessage ?? "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-muted">
          <span>총 {totalElements}건</span>
          <div className="flex items-center gap-2">
            <select
              className="rounded-md border border-ld px-2 py-1"
              data-testid="korus-faculty-sync-page-size-select"
              value={pageSize}
              onChange={(event) => {
                setPageSize(Number(event.target.value) as PageSize);
                setPage(0);
              }}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
            <button
              className="rounded-md border border-ld px-3 py-1"
              data-testid="korus-faculty-sync-prev-button"
              disabled={page === 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              type="button"
            >
              이전
            </button>
            <button
              className="rounded-md border border-ld px-3 py-1"
              data-testid="korus-faculty-sync-next-button"
              disabled={(page + 1) * pageSize >= totalElements}
              onClick={() => setPage((current) => current + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </div>
      </section>
    </section>
  );
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

function StatusBadge({ status }: { status: KorusFacultySyncStatus }) {
  const className =
    status === "SUCCESS"
      ? "bg-lightsuccess text-success"
      : "bg-lighterror text-error";
  return (
    <span
      className={`rounded-full px-2 py-1 text-xs font-semibold ${className}`}
    >
      {status === "SUCCESS" ? "성공" : "실패"}
    </span>
  );
}

function validatePeriod(filters: Filters) {
  const errors: Record<string, string> = {};
  if (!filters.targetStartDate)
    errors.targetStartDate = "대상기간 시작을 입력하세요.";
  if (!filters.targetEndDate)
    errors.targetEndDate = "대상기간 종료를 입력하세요.";
  if (
    filters.targetStartDate &&
    filters.targetEndDate &&
    filters.targetEndDate < filters.targetStartDate
  ) {
    errors.targetEndDate = "대상기간 종료는 시작 이후여야 합니다.";
  }
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
