import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type CourseOperation = {
  achievementId: number;
  managementNo: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  achievementStatus: string;
  teacherName?: string;
};

type SearchResponse = {
  achievements: CourseOperation[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type CourseOperationForm = {
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
};

const initialForm: CourseOperationForm = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
};

/** Renders the approved course-operation list, detail, and guarded save flow. */
export function CourseOperationsPage() {
  const [rows, setRows] = useState<CourseOperation[]>([]);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [form, setForm] = useState<CourseOperationForm>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    setPermissionDenied(false);
    try {
      const listPath =
        `/api/business/course-operations?page=${page}&pageSize=${pageSize}` as `/api/${string}`;
      const response = await apiRequest<SearchResponse>(listPath);
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      handleError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, pageSize]);

  const selectRow = async (row: CourseOperation) => {
    setError(null);
    setFieldErrors({});
    try {
      const detailPath =
        `/api/business/course-operations/${row.achievementId}` as `/api/${string}`;
      const response = await apiRequest<CourseOperation>(detailPath);
      const detail = response.data ?? row;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        performanceDetails: detail.performanceDetails,
      });
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("강좌 개설·운영 실적을 저장하시겠습니까?")) {
      return;
    }
    setSaving(true);
    setError(null);
    setFieldErrors({});
    try {
      const path = (
        selected
          ? `/api/business/course-operations/${selected.achievementId}`
          : "/api/business/course-operations"
      ) as `/api/${string}`;
      const response = await apiRequest<CourseOperation>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          performanceDetails: form.performanceDetails.trim(),
        }),
      });
      if (response.data) {
        setSelected(response.data);
      }
      setSuccess("강좌 개설·운영 실적이 저장되었습니다.");
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermissionDenied(caught.status === 403);
      setError(caught.message);
      setFieldErrors(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "강좌 개설·운영 실적을 처리하지 못했습니다.",
    );
  };

  const locked = selected?.achievementStatus === "EVALUATION_CONFIRMED";
  if (permissionDenied) {
    return (
      <section data-testid="course-operations-page">
        <PermissionState
          title="강좌 개설·운영 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-COURSE-OPERATIONS"
      data-testid="course-operations-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강좌 개설·운영 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              관리항목과 업적발생일, 실적내역을 조회하고 저장합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md border border-primary
              px-4 py-2 text-sm font-semibold text-primary"
            data-testid="course-operations-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </header>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="강좌 개설·운영 실적 오류" message={error} />
      ) : null}

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-operations-search-panel"
      >
        <div className="flex flex-wrap items-end gap-3">
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="course-operations-page-size-select"
              onChange={(event) => {
                setPageSize(Number(event.target.value) as 20 | 50 | 100);
                setPage(0);
              }}
              value={pageSize}
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="course-operations-search-button"
            onClick={() => void load()}
            type="button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-5">
        <h2 className="text-lg font-semibold text-dark">
          강좌 개설·운영 실적 목록
        </h2>
        {loading ? <LoadingState title="강좌 개설·운영 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 강좌 개설·운영 실적이 없습니다"
            message="상세 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">상태</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    data-testid="course-operations-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.achievementStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="course-operations-detail-button"
                        onClick={() => void selectRow(row)}
                        type="button"
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <p className="mt-3 text-xs text-muted">
          총 {total}건 / {page + 1}페이지
        </p>
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5"
        data-testid="course-operations-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          강좌 개설·운영 실적 상세
        </h2>
        {locked ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="course-operations-confirmed-lock-message"
          >
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <label className="text-sm text-muted">
            관리항목 *
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="course-operations-management-item-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
            {fieldErrors.managementItemCode ? (
              <span className="text-error">
                {fieldErrors.managementItemCode}
              </span>
            ) : null}
          </label>
          <label className="text-sm text-muted">
            업적발생일 *
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="course-operations-achievement-date-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
            {fieldErrors.achievementDate ? (
              <span className="text-error">{fieldErrors.achievementDate}</span>
            ) : null}
          </label>
          <label className="text-sm text-muted md:col-span-2">
            실적내역 *
            <textarea
              className="mt-1 min-h-28 w-full rounded-md border border-ld px-3 py-2"
              data-testid="course-operations-performance-details-input"
              disabled={locked}
              onChange={(event) =>
                setForm({ ...form, performanceDetails: event.target.value })
              }
              value={form.performanceDetails}
            />
            {fieldErrors.performanceDetails ? (
              <span className="text-error">
                {fieldErrors.performanceDetails}
              </span>
            ) : null}
          </label>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2
              text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
            data-testid="course-operations-save-button"
            disabled={locked || saving}
            onClick={() => void save()}
            type="button"
          >
            <Save size={16} /> 저장
          </button>
        </div>
      </section>
    </section>
  );
}
