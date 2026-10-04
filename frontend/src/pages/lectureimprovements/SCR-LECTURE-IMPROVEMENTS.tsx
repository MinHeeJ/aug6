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

type LectureImprovement = {
  achievementId: number;
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: number;
  semester: 1 | 2;
  achievementStatus: string;
  attachmentIds?: string | null;
};

type SearchResponse = {
  achievements: LectureImprovement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type SaveResponse = {
  achievement: LectureImprovement;
  occurredDateWarning: boolean;
  warningMessage?: string | null;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: string;
  semester: "1" | "2";
  attachmentIds: string;
};

const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "1",
  attachmentIds: "",
};

const fieldControlClassName = [
  "mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border",
  "[&_input]:border-ld [&_input]:px-3 [&_input]:py-2 [&_input]:text-sm",
  "[&_select]:w-full [&_select]:rounded-md [&_select]:border [&_select]:border-ld",
  "[&_select]:px-3 [&_select]:py-2 [&_textarea]:w-full [&_textarea]:rounded-md",
  "[&_textarea]:border [&_textarea]:border-ld [&_textarea]:px-3 [&_textarea]:py-2",
].join(" ");

/** Renders the BASIC-83 강의개선 목록, 상세 조회, and guarded save workflow. */
export function LectureImprovementsPage() {
  const [rows, setRows] = useState<LectureImprovement[]>([]);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
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
  const confirmed = selected?.achievementStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(
        listPath(page, pageSize),
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

  const selectRow = async (row: LectureImprovement) => {
    try {
      setError(null);
      const response = await apiRequest<LectureImprovement>(
        `/api/business/lecture-improvements/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm(toForm(detail));
      setFieldErrors({});
      setSuccess(null);
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;
    const validationErrors = validateForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setError("필수 입력 항목을 확인하세요.");
      return;
    }
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const method = selected ? "PUT" : "POST";
      const path: `/api/${string}` = selected
        ? `/api/business/lecture-improvements/${selected.achievementId}`
        : "/api/business/lecture-improvements";
      const response = await apiRequest<SaveResponse>(path, {
        method,
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          achievementContent: form.achievementContent.trim(),
          academicYear: Number(form.academicYear),
          semester: Number(form.semester),
          attachmentIds: form.attachmentIds.trim()
            ? form.attachmentIds
                .split(",")
                .map((attachmentId) => attachmentId.trim())
                .filter(Boolean)
            : [],
        }),
      });
      const saved = response.data?.achievement;
      if (saved) {
        setSelected(saved);
        setForm(toForm(saved));
      }
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  const resetForm = () => {
    setSelected(null);
    setForm(emptyForm);
    setFieldErrors({});
    setSuccess(null);
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
        : "강의개선 실적을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-LECTURE-IMPROVEMENTS"
        data-testid="lecture-improvements-page"
      >
        <PermissionState
          title="강의개선 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 해당 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-IMPROVEMENTS"
      data-testid="lecture-improvements-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의개선 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              학년도·학기별 강의개선 내용을 조회하고 저장합니다.
            </p>
          </div>
          <button
            className={
              "inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 " +
              "text-sm font-semibold text-white"
            }
            data-testid="lecture-improvements-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </header>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="강의개선 실적 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="lecture-improvements-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              강의개선 실적 목록
            </h2>
            <p className="mt-1 text-sm text-muted">
              목록에서 상세를 선택하여 수정할 수 있습니다.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <label className="text-sm text-muted">
              표시 건수
              <select
                className="ml-2 rounded-md border border-ld px-2 py-1"
                data-testid="lecture-improvements-page-size-select"
                onChange={(event) => {
                  setPageSize(Number(event.target.value) as 20 | 50 | 100);
                  setPage(0);
                }}
                value={pageSize}
              >
                {[20, 50, 100].map((size) => (
                  <option key={size} value={size}>
                    {size}건
                  </option>
                ))}
              </select>
            </label>
            <button
              className={
                "inline-flex items-center gap-2 rounded-md border border-primary px-3 py-2 " +
                "text-sm font-semibold text-primary"
              }
              data-testid="lecture-improvements-search-button"
              onClick={() => void load()}
              type="button"
            >
              <Search size={16} /> 조회
            </button>
          </div>
        </div>
        {loading ? <LoadingState title="강의개선 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 강의개선 실적이 없습니다"
            message="새 실적을 입력하여 저장하세요."
          />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리항목</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">학년도</th>
                  <th className="px-3 py-2">학기</th>
                  <th className="px-3 py-2">상태</th>
                  <th className="px-3 py-2">상세</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ld">
                {rows.map((row) => (
                  <tr
                    data-testid="lecture-improvements-row"
                    key={row.achievementId}
                  >
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.academicYear}</td>
                    <td className="px-3 py-2">{row.semester}학기</td>
                    <td className="px-3 py-2">{row.achievementStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid={`lecture-improvements-detail-${row.achievementId}`}
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
        data-testid="lecture-improvements-detail-panel"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-dark">
              강의개선 실적 상세
            </h2>
            <p className="mt-1 text-sm text-muted">
              필수값은 서버에서 다시 검증합니다. 평가확정 실적은 수정할 수
              없습니다.
            </p>
          </div>
          <button
            className="rounded border border-ld px-3 py-2 text-sm font-semibold text-link"
            data-testid="lecture-improvements-new-button"
            onClick={resetForm}
            type="button"
          >
            신규 입력
          </button>
        </div>
        {confirmed ? (
          <p
            className="mt-3 text-sm text-error"
            data-testid="lecture-improvements-confirmed-lock"
          >
            평가확정 실적은 수정하거나 첨부를 변경할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            label="관리항목"
            required
            error={fieldErrors.managementItemCode}
          >
            <input
              data-testid="lecture-improvements-management-item-input"
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
              data-testid="lecture-improvements-date-input"
              disabled={confirmed}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
              type="date"
              value={form.achievementDate}
            />
          </Field>
          <Field label="학년도" required error={fieldErrors.academicYear}>
            <input
              data-testid="lecture-improvements-academic-year-input"
              disabled={confirmed}
              min="2000"
              onChange={(event) =>
                setForm({ ...form, academicYear: event.target.value })
              }
              type="number"
              value={form.academicYear}
            />
          </Field>
          <Field label="학기" required error={fieldErrors.semester}>
            <select
              data-testid="lecture-improvements-semester-select"
              disabled={confirmed}
              onChange={(event) =>
                setForm({ ...form, semester: event.target.value as "1" | "2" })
              }
              value={form.semester}
            >
              <option value="1">1학기</option>
              <option value="2">2학기</option>
            </select>
          </Field>
          <Field label="첨부 식별자" error={fieldErrors.attachmentIds}>
            <input
              data-testid="lecture-improvements-attachment-ids-input"
              disabled={confirmed}
              onChange={(event) =>
                setForm({ ...form, attachmentIds: event.target.value })
              }
              placeholder="쉼표로 구분하여 입력"
              value={form.attachmentIds}
            />
          </Field>
          <Field label="상태">
            <input disabled value={selected?.achievementStatus ?? "작성중"} />
          </Field>
          <div className="md:col-span-2">
            <Field
              label="강의개선 내용"
              required
              error={fieldErrors.achievementContent}
            >
              <textarea
                className="min-h-28"
                data-testid="lecture-improvements-content-input"
                disabled={confirmed}
                onChange={(event) =>
                  setForm({ ...form, achievementContent: event.target.value })
                }
                value={form.achievementContent}
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
            data-testid="lecture-improvements-save-button"
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

function listPath(page: number, pageSize: number) {
  return `/api/business/lecture-improvements?page=${page}&pageSize=${pageSize}` as `/api/${string}`;
}

function toForm(row: LectureImprovement): Form {
  return {
    managementItemCode: row.managementItemCode,
    achievementDate: row.achievementDate,
    achievementContent: row.achievementContent,
    academicYear: String(row.academicYear),
    semester: String(row.semester) as "1" | "2",
    attachmentIds: attachmentIdsToText(row.attachmentIds),
  };
}

function attachmentIdsToText(value: string | null | undefined) {
  if (!value) return "";
  try {
    const ids = JSON.parse(value);
    return Array.isArray(ids) ? ids.join(", ") : "";
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
  if (!form.achievementContent.trim())
    errors.achievementContent = "강의개선 내용을 입력하세요.";
  if (!form.academicYear || Number(form.academicYear) < 2000) {
    errors.academicYear = "학년도는 2000 이상이어야 합니다.";
  }
  return errors;
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
      <span className={fieldControlClassName}>{children}</span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
