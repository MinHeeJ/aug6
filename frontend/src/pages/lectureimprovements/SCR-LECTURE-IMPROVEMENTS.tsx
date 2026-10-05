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
  achievementStatus: string;
  achievementContent: string;
  academicYear: number;
  semester: number;
  attachmentIds: string[];
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

/** Faculty screen for search, detail, create, and update of lecture improvements. */
export function LectureImprovementsPage() {
  const [rows, setRows] = useState<LectureImprovement[]>([]);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const locked = selected?.achievementStatus === "EVALUATION_CONFIRMED";

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await apiRequest<SearchResponse>(
        `/api/business/lecture-improvements?page=0&pageSize=${pageSize}` as `/api/${string}`,
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

  const selectRow = async (row: LectureImprovement) => {
    try {
      setError(null);
      const response = await apiRequest<LectureImprovement>(
        `/api/business/lecture-improvements/${row.achievementId}` as `/api/${string}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm(toForm(detail));
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    if (
      !validateForm() ||
      !window.confirm("강의개선 실적을 저장하시겠습니까?")
    ) {
      return;
    }
    try {
      setSaving(true);
      setError(null);
      const body = JSON.stringify({
        managementItemCode: form.managementItemCode.trim(),
        achievementDate: form.achievementDate,
        achievementContent: form.achievementContent.trim(),
        academicYear: Number(form.academicYear),
        semester: Number(form.semester),
        attachmentIds: attachmentIds(form.attachmentIds),
      });
      const path = selected
        ? `/api/business/lecture-improvements/${selected.achievementId}`
        : "/api/business/lecture-improvements";
      const response = await apiRequest<SaveResponse>(
        path as `/api/${string}`,
        {
          method: selected ? "PUT" : "POST",
          body,
        },
      );
      const saved = response.data?.achievement;
      if (saved) {
        setSelected(saved);
        setForm(toForm(saved));
      }
      setSuccess(
        response.data?.occurredDateWarning
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

  const validateForm = () => {
    const nextErrors: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      nextErrors.managementItemCode = "관리항목을 입력하세요.";
    if (!form.achievementDate)
      nextErrors.achievementDate = "업적발생일을 입력하세요.";
    if (!form.achievementContent.trim())
      nextErrors.achievementContent = "실적내용을 입력하세요.";
    if (
      !/^\d{4}$/.test(form.academicYear) ||
      Number(form.academicYear) < 2000
    ) {
      nextErrors.academicYear = "학년도는 2000 이상 네 자리로 입력하세요.";
    }
    setFieldErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
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
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의개선 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              학년도·학기와 실적내용을 입력하고 저장 후 상세값을 재확인합니다.
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
              value={pageSize}
              onChange={(event) =>
                setPageSize(Number(event.target.value) as 20 | 50 | 100)
              }
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
        </div>
        {loading ? <LoadingState title="강의개선 실적 조회 중" /> : null}
        {!loading && rows.length === 0 ? (
          <EmptyState
            title="조회된 강의개선 실적이 없습니다"
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
                  <th className="px-3 py-2">학년도/학기</th>
                  <th className="px-3 py-2">업적발생일</th>
                  <th className="px-3 py-2">인증상태</th>
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
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">
                      {row.academicYear} / {row.semester}
                    </td>
                    <td className="px-3 py-2">{row.achievementDate}</td>
                    <td className="px-3 py-2">{row.achievementStatus}</td>
                    <td className="px-3 py-2">
                      <button
                        className="rounded border border-primary px-2 py-1 text-xs font-semibold text-primary"
                        data-testid="lecture-improvements-detail-button"
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
        data-testid="lecture-improvements-detail-panel"
      >
        <h2 className="text-lg font-semibold text-dark">강의개선 실적 상세</h2>
        <p className="mt-2 text-sm text-muted">
          관리항목, 업적발생일, 실적내용, 학년도, 학기는 필수입니다.
        </p>
        {locked ? (
          <p
            className="mt-2 text-sm text-error"
            data-testid="lecture-improvements-lock-message"
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
              data-testid="lecture-improvements-management-item-input"
              disabled={locked}
              value={form.managementItemCode}
              onChange={(event) =>
                setForm({ ...form, managementItemCode: event.target.value })
              }
            />
          </Field>
          <Field
            label="업적발생일"
            required
            error={fieldErrors.achievementDate}
          >
            <input
              data-testid="lecture-improvements-date-input"
              disabled={locked}
              type="date"
              value={form.achievementDate}
              onChange={(event) =>
                setForm({ ...form, achievementDate: event.target.value })
              }
            />
          </Field>
          <Field label="학년도" required error={fieldErrors.academicYear}>
            <input
              data-testid="lecture-improvements-year-input"
              disabled={locked}
              inputMode="numeric"
              value={form.academicYear}
              onChange={(event) =>
                setForm({ ...form, academicYear: event.target.value })
              }
            />
          </Field>
          <Field label="학기" required error={fieldErrors.semester}>
            <select
              data-testid="lecture-improvements-semester-select"
              disabled={locked}
              value={form.semester}
              onChange={(event) =>
                setForm({
                  ...form,
                  semester: event.target.value as "1" | "2",
                })
              }
            >
              <option value="1">1학기</option>
              <option value="2">2학기</option>
            </select>
          </Field>
          <div className="md:col-span-2">
            <Field
              label="실적내용"
              required
              error={fieldErrors.achievementContent}
            >
              <textarea
                className="min-h-28"
                data-testid="lecture-improvements-content-input"
                disabled={locked}
                value={form.achievementContent}
                onChange={(event) =>
                  setForm({ ...form, achievementContent: event.target.value })
                }
              />
            </Field>
          </div>
          <div className="md:col-span-2">
            <Field label="첨부 참조">
              <input
                data-testid="lecture-improvements-attachments-input"
                disabled={locked}
                value={form.attachmentIds}
                onChange={(event) =>
                  setForm({ ...form, attachmentIds: event.target.value })
                }
              />
            </Field>
          </div>
        </div>
        <div className="mt-5 flex items-center justify-between gap-3">
          <button
            className={
              "inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 " +
              "text-sm font-semibold text-link"
            }
            data-testid="lecture-improvements-new-button"
            onClick={() => {
              setSelected(null);
              setForm(emptyForm);
              setFieldErrors({});
            }}
            type="button"
          >
            <Search size={16} /> 신규 입력
          </button>
          <button
            className={
              "inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 " +
              "text-sm font-semibold text-white disabled:opacity-60"
            }
            data-testid="lecture-improvements-save-button"
            disabled={saving || locked}
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

function toForm(row: LectureImprovement): Form {
  return {
    managementItemCode: row.managementItemCode,
    achievementDate: row.achievementDate,
    achievementContent: row.achievementContent,
    academicYear: String(row.academicYear),
    semester: String(row.semester) as "1" | "2",
    attachmentIds: row.attachmentIds.join(", "),
  };
}

function attachmentIds(value: string) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
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
        className={
          "mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border " +
          "[&_input]:border-ld [&_input]:px-3 [&_input]:py-2 [&_input]:text-sm " +
          "[&_select]:w-full [&_select]:rounded-md [&_select]:border " +
          "[&_select]:border-ld [&_select]:px-3 [&_select]:py-2 [&_textarea]:w-full " +
          "[&_textarea]:rounded-md [&_textarea]:border [&_textarea]:border-ld " +
          "[&_textarea]:px-3 [&_textarea]:py-2 [&_textarea]:text-sm"
        }
      >
        {children}
      </span>
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
