import { RefreshCw, Save } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import { downloadCsv } from "../../utils/exportCsv";

type CourseOperation = {
  achievementId: number;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  achievementStatus: string;
  attachmentIds?: string[];
};

type SearchResponse = {
  achievements: CourseOperation[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  attachmentIds: string;
};

const initialForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
  attachmentIds: "",
};

/** Renders the approved SCR-COURSE-OPERATIONS list, detail, and guarded save flow. */
export function CourseOperationsPage() {
  const [rows, setRows] = useState<CourseOperation[]>([]);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [form, setForm] = useState<Form>(initialForm);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(listPath(pageSize));
      setRows(response.data?.achievements ?? []);
    } catch (caught) {
      handleError(caught);
      setRows([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [pageSize]);

  const selectRow = async (row: CourseOperation) => {
    try {
      const response = await apiRequest<CourseOperation>(
        `/api/business/course-operations/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        performanceDetails: detail.performanceDetails,
        attachmentIds: (detail.attachmentIds ?? []).join(", "),
      });
      setFieldErrors({});
      setSuccess(null);
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("강좌 개설·운영 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const body = {
        managementItemCode: form.managementItemCode.trim(),
        achievementDate: form.achievementDate,
        performanceDetails: form.performanceDetails.trim(),
        attachmentIds: form.attachmentIds
          .split(",")
          .map((value) => value.trim())
          .filter(Boolean),
      };
      const path = selected
        ? `/api/business/course-operations/${selected.achievementId}`
        : "/api/business/course-operations";
      const response = await apiRequest<CourseOperation>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(body),
      });
      setSelected(response.data ?? null);
      setSuccess("저장되었습니다.");
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
        : "강좌 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-COURSE-OPERATIONS"
        data-testid="course-operations-page"
      >
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
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강좌 개설·운영 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              강좌 개설 및 운영 실적내역을 조회하고 저장합니다.
            </p>
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="course-operations-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? (
        <ErrorState title="강좌 개설·운영 실적 오류" message={error} />
      ) : null}

      <section className="rounded-md border border-ld bg-white p-5 shadow-sm">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            강좌 개설·운영 실적 목록
          </h2>
          <div className="flex items-center gap-2">
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                data-testid="course-operations-page-size-select"
                onChange={(event) =>
                  setPageSize(Number(event.target.value) as 20 | 50 | 100)
                }
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
              className="rounded-md border border-ld px-3 py-2 text-sm font-semibold text-link"
              data-testid="course-operations-download-button"
              onClick={() =>
                downloadCsv("course-operations.csv", rows, [
                  { header: "성명", value: (row) => row.teacherName },
                  {
                    header: "관리항목",
                    value: (row) => row.managementItemCode,
                  },
                  { header: "업적발생일", value: (row) => row.achievementDate },
                  {
                    header: "실적내역",
                    value: (row) => row.performanceDetails,
                  },
                  { header: "상태", value: (row) => row.achievementStatus },
                ])
              }
              type="button"
            >
              Excel 다운로드
            </button>
          </div>
        </div>
        {loading ? <LoadingState title="강좌 개설·운영 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 실적이 없습니다"
            message="상세 영역에서 새 실적을 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">성명</th>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">실적내역</th>
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
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.performanceDetails}</td>
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
      </section>

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="course-operations-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          강좌 개설·운영 실적 상세
        </h2>
        <p className="mt-2 text-sm text-muted">
          관리항목, 업적발생일, 실적내역은 필수입니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            error={fieldErrors.managementItemCode}
            label="관리항목"
            required
          >
            <input
              data-testid="course-operations-management-item-input"
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
              value={form.managementItemCode}
            />
          </Field>
          <Field
            error={fieldErrors.achievementDate}
            label="업적발생일"
            required
          >
            <input
              data-testid="course-operations-achievement-date-input"
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Field>
          <div className="md:col-span-2">
            <Field
              error={fieldErrors.performanceDetails}
              label="실적내역"
              required
            >
              <textarea
                className="min-h-28"
                data-testid="course-operations-performance-details-input"
                onChange={(event) =>
                  setForm({ ...form, performanceDetails: event.target.value })
                }
                value={form.performanceDetails}
              />
            </Field>
          </div>
          <Field label="첨부 참조">
            <input
              data-testid="course-operations-attachment-ids-input"
              onChange={(event) =>
                setForm({ ...form, attachmentIds: event.target.value })
              }
              placeholder="첨부 참조를 쉼표로 구분"
              value={form.attachmentIds}
            />
          </Field>
          <Field label="상태">
            <input disabled value={selected?.achievementStatus ?? "작성중"} />
          </Field>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm " +
              "font-semibold text-white disabled:opacity-60"
            }
            data-testid="course-operations-save-button"
            disabled={saving}
            onClick={() => void save()}
            type="button"
          >
            <Save size={16} /> {saving ? "저장 중" : "저장"}
          </button>
        </div>
      </section>
    </section>
  );
}

function listPath(pageSize: number) {
  return `/api/business/course-operations?page=0&pageSize=${pageSize}` as `/api/${string}`;
}

function Field({
  children,
  error,
  label,
  required = false,
}: {
  children: ReactNode;
  error?: string;
  label: string;
  required?: boolean;
}) {
  return (
    <label className="grid gap-1 text-sm text-dark">
      {label}
      {required ? <span className="text-error"> *</span> : null}
      {children}
      {error ? <span className="text-xs text-error">{error}</span> : null}
    </label>
  );
}
