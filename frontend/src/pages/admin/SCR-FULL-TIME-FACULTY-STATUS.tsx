import type React from "react";
import { Download, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  fullTimeFacultyStatusApi,
  type ApiErrorField,
  type FullTimeFacultyStatus,
  type PageSize,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type Filters = {
  baseYear: string;
  organizationCode: string;
  employeeNo: string;
  name: string;
};

const defaultFilters: Filters = {
  baseYear: String(new Date().getFullYear()),
  organizationCode: "",
  employeeNo: "",
  name: "",
};

export function FullTimeFacultyStatusPage() {
  const [filters, setFilters] = useState<Filters>(defaultFilters);
  const [rows, setRows] = useState<FullTimeFacultyStatus[]>([]);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<PageSize>(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const load = async () => {
    const clientErrors = validateFilters(filters);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      setFieldErrors({});
      const response = await fullTimeFacultyStatusApi.listStatuses({
        ...filters,
        page,
        size: pageSize,
      });
      setRows(response.data?.statuses ?? []);
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

  const search = () => {
    setPage(0);
    void load();
  };

  const exportRows = () => {
    downloadCsv("full-time-faculty-statuses.csv", rows, [
      { header: "교번", value: (row) => row.employeeNo },
      { header: "성명", value: (row) => row.name },
      { header: "대학", value: (row) => row.collegeName ?? "" },
      {
        header: "학과",
        value: (row) => row.departmentName ?? row.departmentCode ?? "",
      },
      { header: "직급", value: (row) => row.rankName ?? "" },
      { header: "퇴직일자", value: (row) => row.retirementDate ?? "" },
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
        : "전임교원 현황 조회에 실패했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-FULL-TIME-FACULTY-STATUS"
        data-testid="full-time-faculty-status-page"
      >
        <PermissionState
          title="전임교원 현황 조회 권한이 없습니다"
          message="R03 학과장, R04 업무담당자 또는 R09 시스템관리자 역할과 메뉴 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-FULL-TIME-FACULTY-STATUS"
      data-testid="full-time-faculty-status-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-link">
          교수업적평가 / 기준정보 조회 / 전임교원 현황
        </p>
        <div className="mt-2">
          <h1 className="text-xl font-semibold text-dark">전임교원 현황</h1>
          <p className="mt-2 text-sm text-muted">
            기준연도와 소속 조건으로 KORUS 인사 snapshot의 전임교원
            교번·성명·대학·학과·직급·퇴직일자를 조회합니다.
          </p>
        </div>
      </div>

      <div className="sr-only">
        기준연도 소속 교번 성명 대학 학과 직급 퇴직일자 조회 전용 loading empty
        error permission success 20건 50건 100건
      </div>

      {error ? <ErrorState title="전임교원 현황 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-md"
        data-testid="full-time-faculty-status-filter-panel"
      >
        <div className="grid gap-4 md:grid-cols-4">
          <Field label="기준연도 *" error={fieldErrors.baseYear}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="full-time-faculty-status-base-year-input"
              inputMode="numeric"
              maxLength={4}
              value={filters.baseYear}
              onChange={(event) =>
                setFilters({ ...filters, baseYear: event.target.value })
              }
            />
          </Field>
          <Field label="소속" error={fieldErrors.organizationCode}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="full-time-faculty-status-organization-input"
              placeholder="조직코드"
              value={filters.organizationCode}
              onChange={(event) =>
                setFilters({ ...filters, organizationCode: event.target.value })
              }
            />
          </Field>
          <Field label="교번" error={fieldErrors.employeeNo}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="full-time-faculty-status-employee-input"
              value={filters.employeeNo}
              onChange={(event) =>
                setFilters({ ...filters, employeeNo: event.target.value })
              }
            />
          </Field>
          <Field label="성명" error={fieldErrors.name}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="full-time-faculty-status-name-input"
              value={filters.name}
              onChange={(event) =>
                setFilters({ ...filters, name: event.target.value })
              }
            />
          </Field>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
            data-testid="full-time-faculty-status-search-button"
            onClick={search}
            type="button"
          >
            <Search size={16} /> 조회
          </button>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-muted hover:text-dark"
            data-testid="full-time-faculty-status-reset-button"
            onClick={() => {
              setFilters(defaultFilters);
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
            <h2 className="text-lg font-semibold text-dark">전임교원 목록</h2>
            <p className="mt-1 text-sm text-muted">
              KORUS 원천 인사정보 기반 조회 전용 목록입니다. 등록·수정·제거
              기능은 제공하지 않습니다.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              className="inline-flex h-9 items-center gap-2 rounded-md border border-primary px-3 py-1 text-sm font-semibold text-primary disabled:opacity-50"
              data-testid="full-time-faculty-status-excel-button"
              disabled={rows.length === 0}
              onClick={exportRows}
              type="button"
            >
              <Download size={15} /> 엑셀 내려받기
            </button>
            <span className="rounded-full bg-lightprimary px-3 py-1 text-sm font-semibold text-primary">
              총 {totalElements}건
            </span>
          </div>
        </div>

        {loading ? (
          <LoadingState
            title="전임교원 현황 조회 중"
            message="기준연도 유효 snapshot을 불러오고 있습니다."
          />
        ) : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 전임교원 현황이 없습니다"
            message="기준연도 또는 소속 조건을 변경한 뒤 다시 조회하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightgray text-left text-xs font-semibold uppercase text-muted">
                <tr>
                  <th className="px-3 py-3">교번</th>
                  <th className="px-3 py-3">성명</th>
                  <th className="px-3 py-3">대학</th>
                  <th className="px-3 py-3">학과</th>
                  <th className="px-3 py-3">직급</th>
                  <th className="px-3 py-3">퇴직일자</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    className="hover:bg-lightsecondary"
                    data-testid="full-time-faculty-status-row"
                    key={`${row.employeeNo}-${row.departmentCode ?? "dept"}`}
                  >
                    <td className="px-3 py-3 font-medium text-dark">
                      {row.employeeNo}
                    </td>
                    <td className="px-3 py-3">{row.name}</td>
                    <td className="px-3 py-3">{row.collegeName ?? "-"}</td>
                    <td className="px-3 py-3">
                      {row.departmentName ?? row.departmentCode ?? "-"}
                    </td>
                    <td className="px-3 py-3">{row.rankName ?? "-"}</td>
                    <td className="px-3 py-3">{row.retirementDate ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}

        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-muted">
          <span>페이지 {page + 1}</span>
          <div className="flex items-center gap-2">
            <select
              className="rounded-md border border-ld px-2 py-1"
              data-testid="full-time-faculty-status-page-size-select"
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
              data-testid="full-time-faculty-status-prev-button"
              disabled={page === 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              type="button"
            >
              이전
            </button>
            <button
              className="rounded-md border border-ld px-3 py-1"
              data-testid="full-time-faculty-status-next-button"
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

function validateFilters(filters: Filters) {
  const errors: Record<string, string> = {};
  if (!filters.baseYear.trim()) errors.baseYear = "기준연도는 필수입니다.";
  if (filters.baseYear.trim() && !/^\d{4}$/.test(filters.baseYear.trim())) {
    errors.baseYear = "기준연도는 4자리 숫자여야 합니다.";
  }
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}
