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

type CourseOperation = {
  achievementId: number;
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  performanceDetails: string;
  achievementStatus: string;
  attachmentRefs?: string | null;
};

type ListResponse = {
  achievements: CourseOperation[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type SaveResponse = {
  achievement: CourseOperation;
  occurredDateWarning: boolean;
  warningMessage?: string | null;
};

type Form = {
  achievementId?: number;
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

/** Course-operation search, detail, and R01 save flow for SCR-COURSE-OPERATIONS. */
export function CourseOperationsPage() {
  const [rows, setRows] = useState<CourseOperation[]>([]);
  const [selected, setSelected] = useState<CourseOperation | null>(null);
  const [form, setForm] = useState<Form>(initialForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const confirmed = selected?.achievementStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<ListResponse>(listPath(page, pageSize));
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
        achievementId: detail.achievementId,
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        performanceDetails: detail.performanceDetails,
        attachmentIds: attachmentInput(detail.attachmentRefs),
      });
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    const errors = validateForm(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    if (!window.confirm("강좌 개설·운영 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const path = form.achievementId
        ? `/api/business/course-operations/${form.achievementId}`
        : "/api/business/course-operations";
      const response = await apiRequest<SaveResponse>(path, {
        method: form.achievementId ? "PUT" : "POST",
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          performanceDetails: form.performanceDetails.trim(),
          attachmentIds: form.attachmentIds
            .split(",")
            .map((value) => value.trim())
            .filter(Boolean),
        }),
      });
      const saved = response.data?.achievement;
      if (saved) {
        setSelected(saved);
        setForm({
          achievementId: saved.achievementId,
          managementItemCode: saved.managementItemCode,
          achievementDate: saved.achievementDate,
          performanceDetails: saved.performanceDetails,
          attachmentIds: attachmentInput(saved.attachmentRefs),
        });
      }
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "업적발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
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

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-COURSE-OPERATIONS"
        data-testid="course-operations-page"
      >
        <PermissionState
          title="강좌 개설·운영 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 데이터 범위가 필요합니다."
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
              강좌 개설·운영 실적내역을 저장하고 현재 인증상태를 확인합니다.
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
        data-testid="course-operations-search-panel"
      >
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              강좌 개설·운영 실적 목록
            </h2>
            <p className="mt-1 text-sm text-muted">
              본인 또는 권한 범위의 실적을 최신 발생일 순으로 조회합니다.
            </p>
          </div>
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
                  <th className="px-3 py-2">성명</th>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">인증상태</th>
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
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="course-operations-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">
          강좌 개설·운영 실적 상세
        </h2>
        <p className="mt-2 text-sm text-muted">
          관리항목, 업적발생일, 실적내역은 필수입니다. 평가확정 실적은 수정할 수
          없습니다.
        </p>
        {confirmed ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="course-operations-lock-message"
          >
            평가확정 실적은 수정할 수 없습니다.
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
              disabled={confirmed}
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
              disabled={confirmed}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Field>
          <Field label="첨부 참조" error={fieldErrors.attachmentIds}>
            <input
              data-testid="course-operations-attachment-ids-input"
              disabled={confirmed}
              onChange={(event) =>
                setForm({ ...form, attachmentIds: event.target.value })
              }
              placeholder="첨부 참조를 쉼표로 구분"
              value={form.attachmentIds}
            />
          </Field>
          <Field label="인증상태">
            <input disabled value={selected?.achievementStatus ?? "DRAFT"} />
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
                disabled={confirmed}
                onChange={(event) =>
                  setForm({ ...form, performanceDetails: event.target.value })
                }
                value={form.performanceDetails}
              />
            </Field>
          </div>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm " +
              "font-semibold text-white disabled:opacity-60"
            }
            data-testid="course-operations-save-button"
            disabled={saving || confirmed}
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
    <label className="block text-sm font-medium text-dark">
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <span className="mt-1 block">{children}</span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function listPath(page: number, pageSize: number) {
  const query = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  return `/api/business/course-operations?${query.toString()}` as `/api/${string}`;
}

function attachmentInput(attachmentRefs?: string | null) {
  try {
    const parsed = attachmentRefs ? JSON.parse(attachmentRefs) : [];
    return Array.isArray(parsed) ? parsed.join(", ") : "";
  } catch {
    return "";
  }
}

function validateForm(form: Form) {
  const errors: Record<string, string> = {};
  if (!form.managementItemCode.trim())
    errors.managementItemCode = "관리항목을 입력하세요.";
  if (!form.achievementDate)
    errors.achievementDate = "업적발생일을 입력하세요.";
  if (!form.performanceDetails.trim())
    errors.performanceDetails = "실적내역을 입력하세요.";
  return errors;
}
