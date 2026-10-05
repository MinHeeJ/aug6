import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
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
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName: string;
  performanceDetails: string;
  achievementStatus: string;
  attachmentIds: string;
};

type SearchResponse = {
  achievements: CourseOperation[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type SaveResponse = {
  achievement: CourseOperation;
  achievementDateWarning: boolean;
  warningMessage?: string | null;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  attachmentIds: string;
};

const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  performanceDetails: "",
  attachmentIds: "",
};

/** Course-operation achievement list and detail form backed by the BASIC-83 API. */
export function CourseOperationsPage() {
  const [rows, setRows] = useState<CourseOperation[]>([]);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const editable = selected == null || selected.achievementStatus === "DRAFT";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(
        `/api/business/course-operations?page=${page}&pageSize=${pageSize}`,
      );
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
    try {
      setError(null);
      const response = await apiRequest<CourseOperation>(
        `/api/business/course-operations/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        performanceDetails: detail.performanceDetails,
        attachmentIds:
          detail.attachmentIds === "[]" ? "" : detail.attachmentIds,
      });
      setFieldErrors({});
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
      const attachmentIds = parseAttachmentIds(form.attachmentIds);
      const path = selected
        ? `/api/business/course-operations/${selected.achievementId}`
        : "/api/business/course-operations";
      const response = await apiRequest<SaveResponse>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          performanceDetails: form.performanceDetails.trim(),
          attachmentIds,
        }),
      });
      const saved = response.data?.achievement;
      if (saved) {
        setSelected(saved);
        setForm({
          managementItemCode: saved.managementItemCode,
          achievementDate: saved.achievementDate,
          performanceDetails: saved.performanceDetails,
          attachmentIds:
            saved.attachmentIds === "[]" ? "" : saved.attachmentIds,
        });
      }
      setSuccess(
        response.data?.achievementDateWarning
          ? (response.data.warningMessage ??
              "평가대상 기간 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      if (caught instanceof SyntaxError) {
        setFieldErrors({
          attachmentIds: "첨부 식별자는 JSON 배열 형식이어야 합니다.",
        });
        setError("첨부 식별자를 확인하세요.");
      } else {
        handleError(caught);
      }
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
              실적내역을 확인하고 저장합니다.
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

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="course-operations-list"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            강좌 개설·운영 실적 목록
          </h2>
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
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">성명</th>
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
                    <td className="px-3 py-2">{row.teacherName}</td>
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
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="course-operations-detail-panel"
      >
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              강좌 개설·운영 실적 상세
            </h2>
            <p className="mt-2 text-sm text-muted">
              관리항목, 업적발생일과 실적내역은 필수입니다.
            </p>
          </div>
          <button
            className={[
              "inline-flex items-center gap-2 rounded-md border border-primary px-3 py-2 text-sm",
              "font-semibold text-primary",
            ].join(" ")}
            data-testid="course-operations-new-button"
            onClick={() => {
              setSelected(null);
              setForm(emptyForm);
              setFieldErrors({});
              setSuccess(null);
            }}
            type="button"
          >
            <Search size={16} /> 신규 입력
          </button>
        </div>
        {!editable && selected ? (
          <p
            className="mt-3 text-sm text-error"
            data-testid="course-operations-status-lock-message"
          >
            작성중 상태의 실적만 수정할 수 있습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            label="관리항목"
            required
            error={fieldErrors.managementItemCode}
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
            label="업적발생일"
            required
            error={fieldErrors.achievementDate}
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
              label="실적내역"
              required
              error={fieldErrors.performanceDetails}
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
          <div className="md:col-span-2">
            <Field
              label="첨부 식별자(JSON 배열)"
              error={fieldErrors.attachmentIds}
            >
              <textarea
                data-testid="course-operations-attachment-ids-input"
                onChange={(event) =>
                  setForm({ ...form, attachmentIds: event.target.value })
                }
                value={form.attachmentIds}
              />
            </Field>
          </div>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={[
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm",
              "font-semibold text-white disabled:opacity-60",
            ].join(" ")}
            data-testid="course-operations-save-button"
            disabled={saving || !editable}
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

function parseAttachmentIds(value: string): string[] {
  if (!value.trim()) return [];
  const parsed: unknown = JSON.parse(value);
  if (
    !Array.isArray(parsed) ||
    parsed.some((item) => typeof item !== "string")
  ) {
    throw new SyntaxError("첨부 식별자는 문자열 배열이어야 합니다.");
  }
  return parsed;
}

function Field({
  label,
  required = false,
  error,
  children,
}: {
  label: string;
  required?: boolean;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <span
        className={[
          "mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border",
          "[&_input]:border-ld [&_input]:px-3 [&_input]:py-2 [&_textarea]:w-full",
          "[&_textarea]:rounded-md [&_textarea]:border [&_textarea]:border-ld",
          "[&_textarea]:px-3 [&_textarea]:py-2",
        ].join(" ")}
      >
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
