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
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: number;
  semester: number;
  attachmentIds: string[];
  achievementStatus: string;
};

type SearchResponse = {
  achievements: LectureImprovement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type SaveResponse = {
  achievement: LectureImprovement;
  achievementDateWarning: boolean;
  warningMessage?: string | null;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: string;
  semester: "1" | "2" | "";
  attachmentIds: string;
};

const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "",
  attachmentIds: "",
};

/** Implements the approved faculty screen for lecture-improvement list, detail, and save flows. */
export function LectureImprovementsPage() {
  const [rows, setRows] = useState<LectureImprovement[]>([]);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
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
      const response = await apiRequest<SearchResponse>(
        `/api/business/lecture-improvements?page=0&pageSize=${pageSize}`,
      );
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

  const selectRow = async (row: LectureImprovement) => {
    try {
      const response = await apiRequest<LectureImprovement>(
        `/api/business/lecture-improvements/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm(toForm(detail));
      setSuccess(null);
      setFieldErrors({});
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    const errors = validate(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setError("필수 입력 항목을 확인하세요.");
      return;
    }
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const payload = {
        managementItemCode: form.managementItemCode.trim(),
        achievementDate: form.achievementDate,
        achievementContent: form.achievementContent.trim(),
        academicYear: Number(form.academicYear),
        semester: Number(form.semester),
        attachmentIds: splitAttachmentIds(form.attachmentIds),
      };
      const path = selected
        ? `/api/business/lecture-improvements/${selected.achievementId}`
        : "/api/business/lecture-improvements";
      const response = await apiRequest<SaveResponse>(
        path as `/api/${string}`,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify(payload),
        },
      );
      const saved = response.data?.achievement;
      if (saved) {
        setSelected(saved);
        setForm(toForm(saved));
      }
      setSuccess(
        response.data?.achievementDateWarning
          ? (response.data.warningMessage ??
              "평가대상 기간 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      handleError(caught);
    } finally {
      setSaving(false);
    }
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-LECTURE-IMPROVEMENTS"
        data-testid="lecture-improvements-page"
      >
        <PermissionState
          title="강의개선 실적 관리 권한이 없습니다"
          message="R01, R02 또는 R04 권한과 데이터 범위가 필요합니다."
        />
      </section>
    );
  }

  const confirmed = selected?.achievementStatus === "EVALUATION_CONFIRMED";
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-LECTURE-IMPROVEMENTS"
      data-testid="lecture-improvements-page"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-wrap items-center justify-between gap-3">
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
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            data-testid="lecture-improvements-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="강의개선 실적 오류" message={error} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="lecture-improvements-list-panel"
      >
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-dark">
            강의개선 실적 목록
          </h2>
          <label className="text-sm text-muted">
            표시 건수
            <select
              className="ml-2 rounded-md border border-ld px-2 py-1"
              data-testid="lecture-improvements-page-size-select"
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
              value={pageSize}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="강의개선 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState title="조회된 강의개선 실적이 없습니다" />
        ) : null}
        {!loading && rows.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-ld text-sm">
              <thead className="bg-lightsecondary text-left text-muted">
                <tr>
                  <th className="px-3 py-2">관리번호</th>
                  <th className="px-3 py-2">학년도</th>
                  <th className="px-3 py-2">학기</th>
                  <th className="px-3 py-2">업적발생일</th>
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
                    <td className="px-3 py-2">{row.managementNo}</td>
                    <td className="px-3 py-2">{row.academicYear}</td>
                    <td className="px-3 py-2">{row.semester}학기</td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.achievementStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid={`lecture-improvements-detail-${row.achievementId}`}
                        onClick={() => void selectRow(row)}
                        type="button"
                      >
                        <Search size={14} /> 상세
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
        data-testid="lecture-improvements-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">강의개선 실적 상세</h2>
        {confirmed ? (
          <p className="mt-2 text-sm text-error">
            평가확정 실적은 수정할 수 없습니다.
          </p>
        ) : null}
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            error={fieldErrors.managementItemCode}
            label="관리항목"
            required
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
            error={fieldErrors.achievementDate}
            label="업적발생일"
            required
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
          <Field error={fieldErrors.academicYear} label="학년도" required>
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
          <Field error={fieldErrors.semester} label="학기" required>
            <select
              data-testid="lecture-improvements-semester-select"
              disabled={confirmed}
              onChange={(event) =>
                setForm({
                  ...form,
                  semester: event.target.value as Form["semester"],
                })
              }
              value={form.semester}
            >
              <option value="">선택</option>
              <option value="1">1학기</option>
              <option value="2">2학기</option>
            </select>
          </Field>
          <div className="md:col-span-2">
            <Field
              error={fieldErrors.achievementContent}
              label="강의개선 내용"
              required
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
          <div className="md:col-span-2">
            <Field label="첨부 식별자">
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
          </div>
        </div>
        <div className="mt-5 flex justify-end">
          <button
            className={
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 " +
              "text-sm font-semibold text-white disabled:opacity-60"
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
    <label className="grid gap-1 text-sm text-muted">
      {label}
      {required ? " *" : ""}
      {children}
      {error ? <span className="text-error">{error}</span> : null}
    </label>
  );
}

function toForm(row: LectureImprovement): Form {
  return {
    managementItemCode: row.managementItemCode,
    achievementDate: row.achievementDate,
    achievementContent: row.achievementContent,
    academicYear: String(row.academicYear),
    semester: String(row.semester) as Form["semester"],
    attachmentIds: row.attachmentIds.join(", "),
  };
}

function splitAttachmentIds(value: string) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function validate(form: Form) {
  const errors: Record<string, string> = {};
  if (!form.managementItemCode.trim())
    errors.managementItemCode = "관리항목을 입력하세요.";
  if (!form.achievementDate)
    errors.achievementDate = "업적발생일을 입력하세요.";
  if (!form.achievementContent.trim())
    errors.achievementContent = "강의개선 내용을 입력하세요.";
  if (!form.academicYear || Number(form.academicYear) < 2000)
    errors.academicYear = "학년도를 확인하세요.";
  if (form.semester !== "1" && form.semester !== "2")
    errors.semester = "학기를 선택하세요.";
  return errors;
}
