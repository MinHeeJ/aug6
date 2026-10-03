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

type LectureImprovement = {
  achievementId: number;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: number;
  semester: number;
  achievementStatus: string;
  attachmentRefs?: string;
};

type SearchResponse = {
  achievements: LectureImprovement[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementContent: string;
  academicYear: string;
  semester: string;
  attachmentIds: string;
};

const initialForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementContent: "",
  academicYear: "",
  semester: "",
  attachmentIds: "",
};

/** Renders SCR-LECTURE-IMPROVEMENTS with list, detail, validation, and guarded save states. */
export function LectureImprovementsPage() {
  const [rows, setRows] = useState<LectureImprovement[]>([]);
  const [selected, setSelected] = useState<LectureImprovement | null>(null);
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

  const selectRow = async (row: LectureImprovement) => {
    try {
      const response = await apiRequest<LectureImprovement>(
        `/api/business/lecture-improvements/${row.achievementId}`,
      );
      const detail = response.data ?? row;
      setSelected(detail);
      setForm({
        managementItemCode: detail.managementItemCode,
        achievementDate: detail.achievementDate,
        achievementContent: detail.achievementContent,
        academicYear: String(detail.academicYear),
        semester: String(detail.semester),
        attachmentIds: parseAttachmentRefs(detail.attachmentRefs).join(", "),
      });
      setFieldErrors({});
      setSuccess(null);
    } catch (caught) {
      handleError(caught);
    }
  };

  const save = async () => {
    const localErrors = validateForm(form);
    if (Object.keys(localErrors).length > 0) {
      setFieldErrors(localErrors);
      return;
    }
    if (!window.confirm("강의개선 실적을 저장하시겠습니까?")) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const body = {
        managementItemCode: form.managementItemCode.trim(),
        achievementDate: form.achievementDate,
        achievementContent: form.achievementContent.trim(),
        academicYear: Number(form.academicYear),
        semester: Number(form.semester),
        attachmentIds: form.attachmentIds
          .split(",")
          .map((value) => value.trim())
          .filter(Boolean),
      };
      const path = selected
        ? `/api/business/lecture-improvements/${selected.achievementId}`
        : "/api/business/lecture-improvements";
      const response = await apiRequest<LectureImprovement>(path, {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(body),
      });
      setSelected(response.data ?? null);
      setSuccess("강의개선 실적이 저장되었습니다.");
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
      <section className="rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              강의개선 실적 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              학년도와 학기별 강의개선 실적을 조회하고 저장합니다.
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
      </section>

      {success ? <SuccessState title="처리 완료" message={success} /> : null}
      {error ? <ErrorState title="강의개선 실적 오류" message={error} /> : null}

      <section className="rounded-md border border-ld bg-white p-5 shadow-sm">
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
                  <th className="px-3 py-2">학년도/학기</th>
                  <th className="px-3 py-2">실적내용</th>
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
                    <td className="px-3 py-2">{row.teacherName}</td>
                    <td className="px-3 py-2">{row.managementItemCode}</td>
                    <td className="px-3 py-2">
                      {row.academicYear} / {row.semester}
                    </td>
                    <td className="px-3 py-2">{row.achievementContent}</td>
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
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <Field
            error={fieldErrors.managementItemCode}
            label="관리항목"
            required
          >
            <input
              data-testid="lecture-improvements-management-item-input"
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
              data-testid="lecture-improvements-achievement-date-input"
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
              onChange={(event) =>
                setForm({ ...form, semester: event.target.value })
              }
              value={form.semester}
            >
              <option value="">선택</option>
              <option value="1">1</option>
              <option value="2">2</option>
            </select>
          </Field>
          <div className="md:col-span-2">
            <Field
              error={fieldErrors.achievementContent}
              label="실적내용"
              required
            >
              <textarea
                className="min-h-28"
                data-testid="lecture-improvements-content-input"
                onChange={(event) =>
                  setForm({ ...form, achievementContent: event.target.value })
                }
                value={form.achievementContent}
              />
            </Field>
          </div>
          <Field label="첨부 참조">
            <input
              data-testid="lecture-improvements-attachment-ids-input"
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
            data-testid="lecture-improvements-save-button"
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
  return `/api/business/lecture-improvements?page=0&pageSize=${pageSize}` as `/api/${string}`;
}

function parseAttachmentRefs(attachmentRefs?: string): string[] {
  if (!attachmentRefs) {
    return [];
  }
  try {
    const parsed: unknown = JSON.parse(attachmentRefs);
    return Array.isArray(parsed)
      ? parsed.filter((value): value is string => typeof value === "string")
      : [];
  } catch {
    return [];
  }
}

function validateForm(form: Form): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!form.managementItemCode.trim())
    errors.managementItemCode = "관리항목을 입력하세요.";
  if (!form.achievementDate)
    errors.achievementDate = "업적발생일을 입력하세요.";
  if (!form.achievementContent.trim())
    errors.achievementContent = "실적내용을 입력하세요.";
  if (!form.academicYear || Number(form.academicYear) < 2000) {
    errors.academicYear = "학년도를 확인하세요.";
  }
  if (form.semester !== "1" && form.semester !== "2") {
    errors.semester = "학기를 선택하세요.";
  }
  return errors;
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
      <span>
        {label}
        {required ? <span className="text-error"> *</span> : null}
      </span>
      {children}
      {error ? <span className="text-xs text-error">{error}</span> : null}
    </label>
  );
}
